from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
from tqdm import tqdm

from sacred import Ingredient

import matplotlib.pyplot as plt
import numpy as np
import joblib
import time
import os.path as path

ingredient = Ingredient('randomforest.regression')

def calculate_errors(prediction, target):

    # Euclidean error over all forests and samples
    euklid_error = np.sqrt(np.sum(np.square(prediction - target), 1))
    euklid_error_mean = np.mean(euklid_error)
    euklid_error_std = np.std(euklid_error)
    euklid_error_mean_percent = euklid_error_mean / np.sqrt(2) * 100

    # Daniel: Mean absolute error
    mean_abs_error = np.mean(np.abs(prediction - target), axis=0)
    mean_abs_error_percent = mean_abs_error * 100
    mean_error = np.mean(prediction - target, axis=0)

    
    rmse = np.sqrt(np.mean(np.square(prediction - target), axis=0))

    return  {
        'euklid': {'mean': euklid_error_mean, 'std': euklid_error_std, 'percent': euklid_error_mean_percent}, 
        'mean_abs_error': mean_abs_error, 
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


def fit(sampels, targets, **kwargs):
    number_of_trees = kwargs.get('number_of_trees',   20)
    number_of_cores = kwargs.get('number_of_cores',    1)
    max_tree_depth  = kwargs.get('max_tree_depth',  None)
    oob_score       = kwargs.get('oob_score',       True)

    # initialise random forest regressor
    regressor = RandomForestRegressor(
        n_estimators = number_of_trees, 
        n_jobs       = number_of_cores, 
        max_depth    = max_tree_depth,
        oob_score    = oob_score)


    # train model
    regressor.fit(sampels, targets)

    save = kwargs.get('save', False)
    if save:
        folder = kwargs.get('folder', None)
        if folder is not None:
            prefix = kwargs.get('prefix', '')
            joblib.dump(regressor, path.join(folder, '{0}-rf-regressor.joblib'.format(prefix)))

    return regressor


def test(regressor, sampels, targets):
    prediction = regressor.predict(sampels)
    errors = calculate_errors(prediction, targets)

    return errors, prediction


def single_forest(train_data, test_data, **kwargs):
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
        tuple containing errors on test set (euklid_error_mean, euklid_error_std, mean_abs_error, mean_error, rmse)
    """

    regressor = fit(train_data['samples'], train_data['targets'], **kwargs)
    
    # evaluate performance
    score_training = regressor.score(train_data['samples'], train_data['targets'])
    score_test = regressor.score(test_data['samples'], test_data['samples'])
    score_oob = regressor.oob_score_


    # calculate errors
    errors, prediction = test(regressor, test_data['samples'], test_data['targets'])


    return regressor, prediction, (score_training, score_test, score_oob), errors



def multiple_forests(train_data, test_data, **kwargs):
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

    return regressors, normed_prediction, (score_training, score_test, score_oob), errors