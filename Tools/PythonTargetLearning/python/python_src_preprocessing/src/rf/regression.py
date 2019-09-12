import time
import joblib
import tempfile
import numpy as np
import os.path as path
import matplotlib.pyplot as plt

from tqdm import tqdm
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split

from sacred.ingredient import Ingredient
from sacred.observers.file_storage import FileStorageObserver

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

    return  {
        'euclid': {'mean': euclid_error_mean, 'std': euclid_error_std, 'mean_percent': euclid_error_mean_percent, 'std_percent': euclid_error_std_percent}, 'mean_abs_error': mean_abs_error, 
        'mean_error': mean_error, 
        'rmse': rmse}


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
def fit(sampels, targets, _run, _log, **kwargs):
    number_of_trees = kwargs.get('number_of_trees',   20)
    number_of_cores = kwargs.get('number_of_cores',    1)
    max_tree_depth  = kwargs.get('max_tree_depth',  None)
    oob_score       = kwargs.get('oob_score',       True)
    prefix          = kwargs.get('prefix',            '')

    # initialise random forest regressor
    regressor = RandomForestRegressor(
        n_estimators = number_of_trees, 
        n_jobs       = number_of_cores, 
        max_depth    = max_tree_depth,
        oob_score    = oob_score)

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
def test(regressor, sampels, targets, _run, _log):
    prediction = regressor.predict(sampels)
    errors = calculate_errors(prediction, targets)

    return errors, prediction


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

    regressor = fit(train_data['samples'], train_data['targets'], **kwargs)
    
    # evaluate performance
    score_training = regressor.score(train_data['samples'], train_data['targets'])
    score_test = regressor.score(test_data['samples'], test_data['samples'])
    score_oob = regressor.oob_score_


    # calculate errors
    errors, prediction = test(regressor, test_data['samples'], test_data['targets'])


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
    for i in tqdm(range(0, n_targets), desc='Learning targets', total=n_targets):
        regressor = fit(train_data['samples'],  train_data['targets'][:, i], prefix='target-{0}'.format(i), **kwargs)
        regressors.append(regressor)

        # evaluate performance
        score_training[i] = regressor.score(train_data['samples'], train_data['targets'][:, i])
        score_test[i] = regressor.score(test_data['samples'], test_data['targets'][:, i])
        score_oob[i] = regressor.oob_score_

        prediction = regressor.predict(test_data['samples'])

        predictions[:, i] = prediction

    # calculate errors
    row_sums = predictions.sum(axis=1)
    normed_prediction = predictions / row_sums[:, np.newaxis]

    errors = calculate_errors(normed_prediction, test_data['targets'])

    errors['training_score'] = score_training
    errors['test_score'] = score_test
    errors['oob_score'] = score_oob

    _run.info['error'] = errors

    return regressors, normed_prediction, (score_training, score_test, score_oob), errors