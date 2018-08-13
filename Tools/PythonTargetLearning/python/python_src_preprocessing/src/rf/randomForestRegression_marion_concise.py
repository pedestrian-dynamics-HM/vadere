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
from decimal import Decimal
import math


############# Parameters



def randomForest(test_size_percent, use_cores, directory, numberOfTrees, treeDepths, OBSERVATION_AREA, RESOLUTION):
    #  Some useful length measurements
    imported_files = os.listdir(directory)
    numberOfFiles = len(imported_files)
    numberOfForests = len(numberOfTrees)
    numberOfDepths = len(treeDepths)

    #### Initialization ###
    # getting the dimensions right

    csv_table = np.zeros(shape=(numberOfDepths + 1, numberOfForests * 3 + 1))

    # some labelling for the file
    error_max_trees = pd.DataFrame(csv_table, dtype=object)
    error_mean_trees = pd.DataFrame(csv_table, dtype=object)
    error_max_trees.values[0, 0] = "treeDepth"
    error_mean_trees.values[0, 0] = "treeDepth"

    for idx, x in enumerate(numberOfTrees):
        error_max_trees.values[0, idx + 1] = str(x) + " mean"
        error_max_trees.values[0, idx + numberOfForests + 1] = str(x) + " std"
        error_max_trees.values[0, idx + numberOfForests * 2 + 1] = str(x) + " max"
        error_mean_trees.values[0, idx + 1] = str(x) + " mean"
        error_mean_trees.values[0, idx + numberOfForests + 1] = str(x) + " std"
        error_mean_trees.values[0, idx + numberOfForests * 2 + 1] = str(x) + " max"

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

    #print("Size of data_frame_density:", data_frame_density.shape)
    print("Max value %f" % np.max(np.max(data_frame_density)))
    print("Min value %f" % np.min(np.min(data_frame_density)))

    # determine Target Size
    number_of_targets = determineTargetSize(data_frame_density.iloc[0:10,:])
    
    # split data in samples and response
    y_density = data_frame_density[data_frame_density.columns[-number_of_targets:]]
    x_density = data_frame_density.iloc[:, 0:-number_of_targets]



    # plot input distribution
    plt.scatter( y_density.values[:,0], y_density.values[:,1], label = 'Observation Polygon')
    
    generatedDistributions = pd.read_csv( directory + '/generatedDistributions.txt', header = None)
    plt.scatter(generatedDistributions.values[:,0],generatedDistributions.values[:,1], label = 'generated Distributions')
    plt.legend()
    plt.savefig('C:/Studium/BA/Bilder/scatter.pdf')
    plt.show()
    
    heatmap, xedges, yedges = np.histogram2d(y_density.values[:,0], y_density.values[:,1], bins=50) 
    '''
    for row in range(len(heatmap)):
        for column in range(len(heatmap[row])):
            if heatmap[row][column] == 0:
                heatmap[row][column] = 1.0
    heatmap = np.log(heatmap)
    '''  
    extent = [xedges[0], xedges[-1], yedges[0], yedges[-1]]
    plt.clf()
    plt.imshow(heatmap.T, extent=extent, origin='lower')
    plt.colorbar()
    plt.savefig('C:/Studium/BA/Bilder/heatmap.pdf')
    plt.show()



    # split data into training and test set of data
    x_train_density, x_test_density, y_train_density, y_test_density = train_test_split(x_density, y_density,
                                                                                        random_state=1,
                                                                                        test_size=test_size_percent)
    print('Data is split in test & training set [%.2f s]' % (time.time() - startTime))

    for idf, treeDepth in enumerate(treeDepths):

        error_max_trees.values[idf + 1, 0] = treeDepth
        error_mean_trees.values[idf + 1, 0] = treeDepth

        for idx, x in enumerate(numberOfTrees):
            print("\t Number of Trees ", x)

            startTimeTraining =time.time()
            rf_density_regressor = RandomForestRegressor(n_estimators=x, n_jobs=use_cores, max_depth=treeDepth,
                                                         oob_score=True)

            x_train_density.to_csv("x_train_density.csv")
            y_train_density.to_csv("y_train_density.csv")


            # train model
            rf_density_regressor.fit(x_train_density, y_train_density)
            print('Fitting is done [%.2f s]' % (time.time() - startTime))

            stopTimeTraining = time.time()
            print("Training took : %d seconds" % stopTimeTraining)

            # evaluate model
            y_predicted = rf_density_regressor.predict(x_test_density)
            print('Prediction is done [%.2f s]' % (time.time() - startTime))

            fid1 = open('y_predicted.csv', 'wt')
            y_predicted.tofile(fid1, sep=";")
            fid1.close()

            filename = 'y_test_density' + str(idx) + '.csv'
            y_test_density.to_csv(path_or_buf=filename, sep=";", header=False, index=False)

            # standardization
            row_sums = y_predicted.sum(axis=1)
            y_predicted_normiert = y_predicted / row_sums[:, np.newaxis]

            fid2 = open('y_predicted_normiert.csv', 'wt')
            y_predicted_normiert.tofile(fid2, sep=";")
            fid2.close()

            # calc error of prediction
            # error calculation / writing into my "csv"
            error_mean_trees.values[idf + 1, idx + 1], error_mean_trees.values[idf + 1, numberOfForests + idx + 1], \
            error_mean_trees.values[idf + 1, numberOfForests * 2 + idx + 1] = euklidError(y_test_density,
                                                                                          y_predicted_normiert)
            error_max_trees.values[idf + 1, idx + 1], error_max_trees.values[idf + 1, numberOfForests + idx + 1], \
            error_max_trees.values[idf + 1, numberOfForests * 2 + idx + 1] = maxError(y_test_density,
                                                                                      y_predicted_normiert)

            print("Mean Euklidean Error: %f " % error_mean_trees.values[idf + 1, idx + 1])

            # From intro to machine learning book
            # Returns the coefficient of determination R^2 of the prediction.
            print("Accuracy on training set (Score): {:.3f}".format(
                rf_density_regressor.score(x_train_density, y_train_density)))
            print("Accuracy on test set (Score): {:.3f}".format(
                rf_density_regressor.score(x_test_density, y_test_density)))

    ############### writing the csv one table for euklid error and one for max error.
    error_mean_trees.to_csv(path_or_buf="auswertung_euklid.csv", sep=';', header=False, index=False)
    error_max_trees.to_csv(path_or_buf="auswertung_max.csv", sep=';', header=False, index=False)

    ###################### eval of the trees


    print("Feature importance -> plot ")

    print(type(rf_density_regressor.feature_importances_))
    print(np.size(rf_density_regressor.feature_importances_))
    n = int(np.sqrt(len(rf_density_regressor.feature_importances_)))
    features_importance = np.reshape(rf_density_regressor.feature_importances_, [int(OBSERVATION_AREA[-1]/RESOLUTION), int(OBSERVATION_AREA[-2]/RESOLUTION)])
    
    print(np.size(features_importance))

    plt.figure()
    plt.imshow(features_importance)
    plt.title("Feature importance")
    plt.colorbar()
    plt.show()

    print("Out of bag error score %f" % rf_density_regressor.oob_score_)


    print(
        "Accuracy on training set (Score): {:.3f}".format(rf_density_regressor.score(x_train_density, y_train_density)))
    print("Accuracy on test set (Score): {:.3f}".format(rf_density_regressor.score(x_test_density, y_test_density)))

    ## Visualize tree
    '''
    for tree_in_forest in rf_density_regressor.estimators_:
        export_graphviz(tree_in_forest,
                        out_file="tree.dot")

    os.system('dot -Tpng tree.dot -o tree.png')
    print("visusalized tree.")
    '''
    return None


############## Helper Functions
    
def determineTargetSize(data):
    target_set = set()
    
    data = data.iloc[:, ::-1]
    for i in range(len(data.index)):
        sum = 0
        target = 0
        switch_done = False
        for j in range(len(data.columns)):
            sum += data.values[i,j]
            
            if math.isclose(sum, 1,abs_tol = 1e-10):
                switch_done = True
                
            target += 1
            
            if switch_done:
                target_set.add(target)
                break

    if len(target_set) > 1:
        raise ValueError('The Algorithm to determine the Number of Targets has failed, or the data is corrupt')
    number_of_targets = target_set.pop()
    print('Number of Targets Calculated: ' , number_of_targets)
    return number_of_targets
    

def euklidError(y_data, y_predicted):
    if len(y_data) != len(y_predicted):
        print("dimensions dont match up")
        return

    y_error = np.zeros(len(y_data))

    for i in range(0, len(y_data.index)):
        tmpsum = 0
        for j in range(0, len(y_data.columns)):
            tmpsum += (y_data.values[i,j] - y_predicted[i,j])**2

        y_error[i] = np.sqrt(tmpsum)/np.sqrt(2) # for each sample

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

        y_error[i] = np.max(tmpsum)/np.sqrt(2)

    return np.mean(y_error), np.std(y_error), np.max(y_error)



if __name__ == '__main__':

    test_size_percent = 0.2
    use_cores = 4

    directory = '../../data/output_preprocessed/'

    numberOfTrees = [10]
    treeDepths = [None] # as long as possible

    randomForest(test_size_percent, use_cores, directory, numberOfTrees, treeDepths)
          
          
          
