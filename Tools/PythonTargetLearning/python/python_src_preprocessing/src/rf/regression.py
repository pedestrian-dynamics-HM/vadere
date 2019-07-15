from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
import matplotlib.pyplot as plt
import numpy as np
import joblib
import time
import os.path as path

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

    return  (euklid_error_mean, euklid_error_std, mean_abs_error, mean_error, rmse)
    

def features_importance(regressor, area):
    return np.reshape(regressor.feature_importances_, area)


def plot_features_importance(regressor, area, title, path):
    fi = features_importance(regressor, area)

    fig, ax = plt.subplots()
    fig.sup_title(title)
    img = ax.imshow(fi)
    fig.colorbar(img)
    fig.savefig(path)


def train(sampels, targets, **kwargs):
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


def single_forest(samples, targets, **kwargs):
    """Train using single forest 


    Parameters
    ----------
    samples : array
        3 dimentional array with n samples of dimension (m x k)

    targets : array
        2 dimentional (n x l) array with l targets for each of the n samples

    number_of_trees : int
        number of trees to be used in forest (default: 20)

    number_of_cores : int
        number of cpu cores to use for training  (default: 1)

    test_size : double
        percentage of samples to be used as test data, must be between 0 and 1 (default: 0.2)
    
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

    # init options
    test_size = kwargs.get('test_size', 0.2)

    # split samples into training and test
    train_samples, test_samples, train_targets, test_targets = \
        train_test_split(samples, targets, shuffle = True, test_size = test_size)
    
    regressor = train(train_samples, train_targets, **kwargs)
    
    # evaluate performance
    score_training = regressor.score(train_samples, train_targets)
    score_test = regressor.score(test_samples, test_samples)
    score_oob = regressor.oob_score_


    # calculate errors
    errors, prediction = test(regressor, test_samples, test_targets)


    return regressor, prediction, (score_training, score_test, score_oob), errors



def multiple_forests(samples, targets, **kwargs):
    """Train using one forest per target

    Parameters
    ----------
    samples : array
        3 dimentional array with n samples of dimension (m x k)

    targets : array
        2 dimentional (n x l) array with l targets for each of the n samples

    number_of_trees : int
        number of trees to be used in forest (default: 20)

    number_of_cores : int
        number of cpu cores to use for training  (default: 1)

    test_size : double
        percentage of samples to be used as test data, must be between 0 and 1 (default: 0.2)
    
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

    # init options
    test_size       = kwargs.get('test_size',        0.2)
    
    # split samples into training and test
    train_samples, test_samples, train_targets, test_targets = \
        train_test_split(samples, targets, shuffle = True, test_size = test_size)

    # number of targets
    n_targets = len(targets[0])

    regressors = []
    predictions = np.zeros([len(test_targets), n_targets]) 
    score_test = np.zeros(n_targets)
    score_training = np.zeros(n_targets)
    score_oob = np.zeros(n_targets)

    # train single forest for each target
    for i in range(0, n_targets):
        regressor = train(train_samples, train_targets, prefix='target-{0}'.format(i), **kwargs)
        regressors.append(regressor)

        # evaluate performance
        score_training[i] = regressor.score(train_samples, train_targets)
        score_test[i] = regressor.score(test_samples, test_samples)
        score_oob[i] = regressor.oob_score_

        _, prediction = test(regressor, test_samples, test_targets[:, i])

        predictions[:, i] = prediction

    # calculate errors
    row_sums = predictions.sum(axis=1)
    normed_prediction = predictions / row_sums[:, np.newaxis]

    errors = calculate_errors(normed_prediction, test_targets)

    return regressors, normed_prediction, (score_training, score_test, score_oob), errors