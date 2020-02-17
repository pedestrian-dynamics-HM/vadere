import time
import joblib
import tempfile
import numpy as np
import os.path as path
import matplotlib.pyplot as plt

from functools import reduce

from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split

from sacred.ingredient import Ingredient
from utils.sacred.file_storage import FileStorageObserver

ingredient = Ingredient('randomforest.regression')


def calculate_errors(prediction, target):
    # Euclidean error over all forests and samples
    euclid_error = np.sqrt(np.sum(np.square(prediction - target), 1))
    euclid_error_mean = np.mean(euclid_error)
    euclid_error_std = np.std(euclid_error)
    euclid_error_mean_percent = euclid_error_mean / np.sqrt(2) * 100
    euclid_error_std_percent = euclid_error_std / np.sqrt(2) * 100

    # Daniel: Mean absolute error
    mean_abs_error = np.mean(np.abs(prediction - target), axis=0)
    mean_abs_error_percent = mean_abs_error * 100
    mean_error = np.mean(prediction - target, axis=0)

    rmse = np.sqrt(np.mean(np.square(prediction - target), axis=0))

    return {
        'euclid': {'mean': euclid_error_mean, 'std': euclid_error_std, 'mean_percent': euclid_error_mean_percent,
                   'std_percent': euclid_error_std_percent},
        'mean_abs_error': list(mean_abs_error),
        'mean_error': list(mean_error),
        'rmse': list(rmse)}


# Computes the absolute and relative error for the prediction of an od-matrix
#
# abs : L2|target-prediction|
# rel : abs/(ped_count) (use the ped count of the ground truth to normalize)
#
# Returns
# dict :   {absolute errors over all samples,
#           abs errors of every sample,
#           relative errors of all samples,
#           relative errors per sample
#          }
def od_matrix_error(targets, predictions):
    diff = np.subtract(targets, predictions)
    error_abs_samples = np.sum(np.abs(diff) ** 2, axis=-1) ** (1. / 2)  # L2 normalization

    mean_error_abs = np.mean(error_abs_samples)
    min_error_abs = np.min(error_abs_samples)
    max_error_abs = np.max(error_abs_samples)
    std_error_abs = np.std(error_abs_samples)

    # Compute relative errors by normalizing with the current
    # pedestrian count of the ground truth od matrix
    ped_count = np.sum(targets, axis=1)
    error_rel_samples = np.divide(error_abs_samples, ped_count)

    mean_error_rel = np.mean(error_rel_samples)
    min_error_rel = np.min(error_rel_samples)
    max_error_rel = np.max(error_rel_samples)
    std_error_rel = np.std(error_rel_samples)

    print(error_abs_samples, '\n', error_rel_samples)

    abs_errors = {'mean error': mean_error_abs, 'min error': min_error_abs, 'max error': max_error_abs,
                  'std error': std_error_abs}
    rel_errors = {'mean error': mean_error_rel, 'min error': min_error_rel, 'max error': max_error_rel,
                  'std error': std_error_rel}

    return {'abs error': abs_errors,
            'abs error per sample': list(error_abs_samples),
            'rel error per sample': list(error_rel_samples),
            'rel error': rel_errors}


def features_importance(regressor, area):
    return np.reshape(regressor.feature_importances_, area)


def plot_features_importance(regressor, area, title, path):
    fi = features_importance(regressor, area)

    fig, ax = plt.subplots()
    fig.sup_title(title)
    img = ax.imshow(fi)
    fig.colorbar(img)
    fig.savefig(path)


@ingredient.capture
def fit_regressor(sampels, targets, _run, _log, **kwargs):
    number_of_trees = kwargs.get('number_of_trees', 20)
    number_of_cores = kwargs.get('number_of_cores', 1)
    max_tree_depth = kwargs.get('max_tree_depth', None)
    oob_score = kwargs.get('oob_score', True)
    prefix = kwargs.get('prefix', '')

    # initialise random forest regressor
    regressor = RandomForestRegressor(
        n_estimators=number_of_trees,
        n_jobs=number_of_cores,
        max_depth=max_tree_depth,
        oob_score=oob_score)

    # train model
    start = time.time_ns()
    _log.info("Start fitting model {}".format(prefix))
    regressor.fit(sampels, targets)
    elapsed = time.time_ns() - start
    _log.info("Done fitting model {} - {}ns needed".format(prefix, elapsed))

    info = {
        'prefix': prefix,
        'number_of_samples': len(sampels),
        'number_of_features': len(sampels[0]),
        'number_of_cores': number_of_cores,
        'number_of_trees': number_of_trees,
        'max_tree_depth': max_tree_depth,
        'use_oob_score': oob_score,
        'training_time': elapsed
    }

    save = kwargs.get('save', False)
    if save:
        folder = kwargs.get('folder', None)

        if folder is None:
            for obs in _run.observers:
                if isinstance(obs, FileStorageObserver):
                    folder = obs.dir
                    break

        model_name = '{0}-rf-regressor.joblib'.format(prefix)

        joblib.dump(regressor, path.join(tempfile.gettempdir(), model_name))
        _run.add_artifact(path.join(tempfile.gettempdir(), model_name), model_name)
        info['model_path'] = path.join(folder, model_name)

    if _run.info.get('training') is None:
        _run.info['training'] = info
    elif isinstance(_run.info.get('training'), list):
        _run.info.get('training').append(info)

    return regressor


@ingredient.capture
def single_forest(train_data, test_data, _run, _log, **kwargs):
    """Train using single forest


    Parameters
    ----------
    train_data : dict
        training dataset, dictionary containing keys 'samples' and 'targets' of type np.array

    test_data : dict
        test dataset, dictionary containing keys 'samples' and 'targets' of type np.array

    number_of_trees : int
        number of trees to be used in forest (default: 20)

    number_of_cores : int
        number of cpu cores to use for training  (default: 1)

    max_tree_depth : int
        maximal depth of tree (default: None)

    oob_score : boolean
        record out of bag score (default: True)

    save : boolean
        save regression model to file (default: False)

    folder : string
        folder to save model to if save == True (default: None)

    prefix : string
        prefix used for filename of model (default: '')

    Returns
    -------
    RandomForestRegressor
        random forest regressor
    array
        predictions on test set
    tuple
        tuple containing model scores (training score, test score, out of bag score)
    tuple
        tuple containing errors on test set (euclid_error_mean, euclid_error_std, mean_abs_error, mean_error, rmse)
    """

    regressor = fit_regressor(train_data['samples'], train_data['targets'], **kwargs)

    # get predictions
    prediction = regressor.predict(test_data['samples'])

    # write predictions and ground truth (targets of test data) to file
    prediction_path = path.join(tempfile.gettempdir(), 'prediction.csv')
    np.savetxt(prediction_path, np.concatenate((prediction, test_data['targets']), axis=1), fmt='%.5e', delimiter=',')
    _run.add_artifact(prediction_path, 'predictions.csv')

    # evaluate performance
    score_training = regressor.score(train_data['samples'], train_data['targets'])
    score_test = regressor.score(test_data['samples'], test_data['targets'])
    score_oob = regressor.oob_score_

    # calculate errors differently depending on prediction model type
    prediction_model = kwargs.get('prediction_model', None)

    if prediction_model == 'destination-vector':
        errors = calculate_errors(prediction, test_data['targets'])
        errors['training_score'] = score_training
        errors['test_score'] = score_test
        errors['oob_score'] = score_oob

    elif prediction_model == 'od-matrix':
        errors = od_matrix_error(prediction, test_data['targets'])
        errors['training_score'] = score_training
        errors['test_score'] = score_test
        errors['oob_score'] = score_oob
    else:
        raise NotImplemented

    _run.info['error'] = errors

    return regressor, prediction, (score_training, score_test, score_oob), errors


@ingredient.capture
def multiple_forests(train_data, test_data, _run, _log, **kwargs):
    """Train using one forest per target

    Parameters
    ----------
    train_data : dict
        training dataset, dictionary containing keys 'samples' and 'targets' of type np.array

    test_data : dict
        test dataset, dictionary containing keys 'samples' and 'targets' of type np.array

    number_of_trees : int
        number of trees to be used in forest (default: 20)

    number_of_cores : int
        number of cpu cores to use for training  (default: 1)

    max_tree_depth : int
        maximal depth of tree (default: None)

    oob_score : boolean
        record out of bag score (default: True)

    save : boolean
        save regression model to file (default: False)

    folder : string
        folder to save model to if save == True (default: None)

    Returns
    -------

    """

    # number of targets
    n_targets = kwargs.get('number_of_targets', None)

    if n_targets is None:
        # if not provided look at data
        n_targets = len(train_data['targets'][0])

    regressors = []
    predictions = np.zeros([len(test_data['targets']), n_targets])
    score_test = np.zeros(n_targets)
    score_training = np.zeros(n_targets)
    score_oob = np.zeros(n_targets)

    _run.info['setup'] = {
        'number_of_samples_training': len(train_data['samples']),
        'number_of_samples_testing': len(test_data['samples']),
        'number_of_features': len(train_data['samples'][0]),
        'number_of_targets': n_targets
    }

    _run.info['training'] = []

    # train single forest for each target
    for i in range(0, n_targets):
        regressor = fit_regressor(train_data['samples'], train_data['targets'][:, i], prefix='target-{0}'.format(i),
                                  **kwargs)
        regressors.append(regressor)

        # evaluate performance
        score_training[i] = regressor.score(train_data['samples'], train_data['targets'][:, i])
        score_test[i] = regressor.score(test_data['samples'], test_data['targets'][:, i])
        score_oob[i] = regressor.oob_score_

        prediction = regressor.predict(test_data['samples'])

        predictions[:, i] = prediction

    # save predictions
    prediction_path = path.join(tempfile.gettempdir(), 'prediction.csv')
    prediction_model = kwargs.get('prediction_model', None)

    # TODO check how should error for od-matrix prediction be normalized?
    if prediction_model == 'destination-vector':
        row_sums = predictions.sum(axis=1)  # norm predictions to 1 when predicting percentages
        predictions = predictions / row_sums[:, np.newaxis]
        errors = od_matrix_error(predictions, test_data['targets'])
        errors['training_score'] = list(score_training)
        errors['test_score'] = list(score_test)
        errors['oob_score'] = list(score_oob)
        # save predictions in csv file
        it = [''] + list(range(n_targets))[::-1]
        header = reduce(lambda s, x: 'prediction-' + str(x) + ',' + str(s), it) + reduce(
            lambda s, x: 'target-' + str(x) + ',' + str(s), it)
        np.savetxt(prediction_path, np.concatenate((predictions, test_data['targets']), axis=1), fmt='%.5e',
                   delimiter=',', header=header)
    elif prediction_model == 'od-matrix':
        errors = calculate_errors(prediction, test_data['targets'])
        errors['training_score'] = list(score_training)
        errors['test_score'] = list(score_test)
        errors['oob_score'] = list(score_oob)
        header = ['p{0}'.format(i) for i in range(n_targets)] + ['t{0}'.format(i) for i in range(n_targets)]
        header = ','.join(header)
        np.savetxt(prediction_path, np.concatenate((predictions, test_data['targets']), axis=1), fmt='%.5e',
                   delimiter=',', header=header)

    else:
        raise NotImplemented

    _run.add_artifact(prediction_path, 'predictions.csv')
    _run.info['error'] = errors

    return regressors, normed_prediction, (score_training, score_test, score_oob), errors
