# -*- coding: utf-8 -*-
"""
Created on Sun Oct 22 13:56:09 2017

@author: Tim Lauster

"""

from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
from sklearn.feature_selection import SelectFromModel
from matplotlib import pyplot as plt
#from sklearn.model_selection import cross_val_score
import pandas as pd
import numpy as np
import time
import os
import re
import mglearn
from sklearn.tree import export_graphviz


############# Parameters



def randomForest(test_size_percent, use_cores, directory, numberOfTrees, treeDepth):


    print("*** SINGLE FOREST ***")

    #  Some useful length measurements
    imported_files = os.listdir(directory)



    startTime = time.time()

    # import data
    data_frame_density = pd.DataFrame()
    for ifile in range(0, len(imported_files)):
        file = imported_files[ifile]
        if file.endswith(".csv"):
            # read in data
            tmp_density = pd.read_csv(directory + file, header=None, sep=';')
            data_frame_density = data_frame_density.append(tmp_density)
            if np.mod(ifile, 10) == 0:
                print("Reading files: ", round(ifile / len(imported_files) * 100), "%")

    print("Size of data_frame_density:", data_frame_density.shape)


    # split data in samples and response
    y_density = data_frame_density[data_frame_density.columns[-3:]]
    x_density = data_frame_density.iloc[:, 0:-3]

    # round to 1%
    #y_density = np.round(y_density*100)/100

    # split data into training and test set of data
    x_train_density, x_test_density, y_train_density, y_test_density = train_test_split(x_density, y_density,
                                                                                        random_state=1,
                                                                                        test_size=test_size_percent)
    print('Data is split in test & training set [%.2f s]' % (time.time() - startTime))


    print("\t Number of Trees ", numberOfTrees)

    startTime =time.time()
    rf_density_regressor = RandomForestRegressor(n_estimators=numberOfTrees, n_jobs=use_cores, max_depth=treeDepth,
                                                 oob_score=True)

    x_train_density.to_csv("x_train_density.txt")
    y_train_density.to_csv("y_train_density.txt")


    # train model
    rf_density_regressor.fit(x_train_density, y_train_density)
    print('Fitting is done [%.2f s]' % (time.time() - startTime))
    # print(np.shape(x_train_density))
    # print(np.shape(y_train_density))


    startTime =time.time()
    # evaluate model
    y_predicted = rf_density_regressor.predict(x_test_density)
    print('Prediction is done [%.2f s]' % (time.time() - startTime))
    # print(np.shape(x_test_density))
    # print(np.shape(y_predicted))

    fid1 = open('y_predicted.csv', 'wt')
    y_predicted.tofile(fid1, sep=";")
    fid1.close()

    filename = 'y_test_density' + '.csv'
    y_test_density.to_csv(path_or_buf=filename, sep=";", header=False, index=False)

    # standardization
    row_sums = y_predicted.sum(axis=1)
    y_predicted_normiert = y_predicted / row_sums[:, np.newaxis]

    fid2 = open('y_predicted_normiert.csv', 'wt')
    y_predicted_normiert.tofile(fid2, sep=";")
    fid2.close()

    # calc error of prediction

    # print importance of features
    # print("Importance of features")
    # print(rf_density_regressor.feature_importances_)

    # error calculation / writing into my "csv"
    mean_euklid_error,mean_euklid_error_std,mean_euklid_error_max = euklidError(y_test_density, y_predicted_normiert)
    mean_max_error, mean_max_error_std, mean_max_error_max = maxError(y_test_density,
                                                                              y_predicted_normiert)
    mean_euklid_error_percent = mean_euklid_error/np.sqrt(2)*100


    euklid_error = np.sqrt(np.sum(np.square(y_predicted_normiert - np.array(y_test_density)), 1))
    euklid_error_mean = np.mean(euklid_error)
    euklid_error_mean_percent = euklid_error_mean / np.sqrt(2) * 100

    print("**** RESULTS ****")


    print("Mean Euklidean Error (Test data) - Students: %f " % mean_euklid_error)
    print("Mean Euklidean Error (Test data) - Students: %f %%" % mean_euklid_error_percent)
    print(" ")
    print("Mean Euklidean Error (Test data): %f" % euklid_error_mean)
    print("Mean Euklidean Error (Test data): %f %%" % euklid_error_mean_percent)
    print(" ")


    # From intro to machine learning book
    # Returns the coefficient of determination R^2 of the prediction.
    print("Accuracy on training set (Score): {:.3f}".format(
        rf_density_regressor.score(x_train_density, y_train_density)))
    print("Accuracy on test set (Score): {:.3f}".format(
        rf_density_regressor.score(x_test_density, y_test_density)))

    print("Out of bag error score %f" % rf_density_regressor.oob_score_)


    ###################### eval of the trees


    n = int(np.sqrt(len(rf_density_regressor.feature_importances_)))
    features_importance = np.reshape(rf_density_regressor.feature_importances_, [n, n])

    # print(type(rf_density_regressor.feature_importances_))
    # print(np.size(rf_density_regressor.feature_importances_))
    # print(np.size(features_importance))

    plt.figure()
    plt.imshow(features_importance)
    plt.title("Feature importance")
    plt.colorbar()
    plt.show()

    # print("Out of bag error prediction")
    # print(rf_density_regressor.oob_prediction_)

    print(" ")
    # print("Estimators")
    # print(rf_density_regressor.estimators_)


    ## Visualize tree
    # for tree_in_forest in rf_density_regressor.estimators_:
    #     export_graphviz(tree_in_forest,
    #                    out_file="tree.dot")

    # os.system('dot -Tpng tree.dot -o tree.png')
    # print("visusalized tree.")

    return None


############## Helper Functions for error calculation
def euklidError(y_data, y_predicted):
    if len(y_data) != len(y_predicted):
        print("dimensions dont match up")
        return

    y_error = np.zeros(len(y_data))

    for i in range(0, len(y_data.index)):
        tmpsum = 0
        for j in range(0, len(y_data.columns)):
            tmpsum += (y_data.values[i,j] - y_predicted[i,j])**2

        y_error[i] = np.sqrt(tmpsum) # for each sample

    fid1 = open('y_error.csv', 'wt')
    y_error.tofile(fid1, sep=";")
    fid1.close()


    return np.mean(y_error), np.std(y_error), np.max(y_error)

def maxError(y_data, y_predicted):
    if len(y_data) != len(y_predicted):
        print("dimensions dont match up")
        return None

    y_error = np.zeros(len(y_data))

    for i in range(0, len(y_data.index)):
        tmpsum = np.zeros(shape = (3))
        for j in range(0, len(y_data.columns)):
            tmpsum[j] = (y_data.values[i,j] - y_predicted[i,j])

        y_error[i] = np.max(tmpsum)

    return np.mean(y_error), np.std(y_error), np.max(y_error)



if __name__ == '__main__':

    test_size_percent = 0.2
    use_cores = 4

    directory = '../../data/output_preprocessed/'

    numberOfTrees = [10]
    treeDepths = [None] # as long as possible

    randomForest(test_size_percent, use_cores, directory, numberOfTrees, treeDepths)
          
          
          
