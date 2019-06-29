from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
import numpy as np
import time

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

    return  euklid_error_mean, euklid_error_std, mean_abs_error, mean_error, rmse
    



def single(samples, targets, **kwargs):
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

    Returns
    -------
    array
        predictions 
    double
    
    double
    
    double
    """

    # init options
    number_of_trees = kwargs.get('number_of_trees',   20)
    number_of_cores = kwargs.get('number_of_cores',    1)
    test_size       = kwargs.get('test_size',        0.2)
    max_tree_depth  = kwargs.get('max_tree_depth',  None)
    oob_score       = kwargs.get('oob_score',       True)

    # split samples into training and test
    train_samples, test_samples, train_targets, test_targets = \
        train_test_split(samples, targets, shuffle = True, test_size = test_size)

    # initialise random forest regressor
    regressor = RandomForestRegressor(
        n_estimators = number_of_trees, 
        n_jobs       = number_of_cores, 
        max_depth    = max_tree_depth,
        oob_score    = oob_score)


    # train model
    regressor.fit(train_samples, train_targets)
        
    
    # evaluate model
    prediction = regressor.predict(test_samples)
    
    # evaluate performance
    score_training = regressor.score(train_samples, train_targets)
    score_test = regressor.score(test_samples, test_samples)
    score_oob = regressor.oob_score_


    # calculate errors
    calculate_errors(prediction, test_targets)


    return prediction, score_training, score_test, score_oob



def multiple(samples, targets, **kwargs):
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


    Returns
    -------

    """

    # number of targets
    n_targets = len(targets[0])

    # train single forest for each target
    for i in range(0, n_targets):
        single(samples, targets[:, i])


"""
def train():

    # standardization
    row_sums = y_predicted.sum(axis=1)
    y_predicted_normiert = y_predicted / row_sums[:, np.newaxis]

    combined = np.zeros((len(y_predicted_normiert), 6))
    combined[:, :3] = y_predicted_normiert
    combined[:, 3:] = y_test_density_np

    calc_and_print_errors_rf(y_test_density_np, y_predicted_normiert, log_file, score_training, score_test, score_oob)

    n = int(np.sqrt(len(rf_density_regressor.feature_importances_)))
    features_importance = np.reshape(rf_density_regressor.feature_importances_, [int(obs_area[3]/resolution),int(obs_area[2]/resolution)])
"""