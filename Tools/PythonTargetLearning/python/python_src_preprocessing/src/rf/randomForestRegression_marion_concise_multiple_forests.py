# -*- coding: utf-8 -*-
"""
Created on Sun Oct 22 13:56:09 2017

@author: Tim Lauster

"""

from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
from matplotlib import pyplot as plt
import pandas as pd
import numpy as np
import time
import os
from rf.error_calculation import calc_and_print_errors_rf


# Parameters



def randomForest(test_size_percent, use_cores, directory, numberOfTrees, treeDepth, nTargets, log_file, log_file_name, obs_area, resolution):

    print("*** MULTIPLE FORESTS ***")

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
                                                                                        #random_state=1,
                                                                                        shuffle=True,
                                                                                        test_size=test_size_percent)
    print('Data is split in test & training set [%.2f s]' % (time.time() - startTime))
    #print('Size train: %d %d'% (x_train_density.shape(0),x_train_density.shape(1)))
    #print('Size test: %d %d'% (x_test_density.shape(0),x_test_density.shape(1)))



    print("\t Number of Trees ", numberOfTrees)

    n_test = y_test_density.shape
    y_test_density_np = y_test_density._get_values
    y_predicted = np.zeros([n_test[0],n_test[1]]) # empty numpy ndarray
    score_test = np.zeros(nTargets)
    score_training = np.zeros(nTargets)
    score_oob = np.zeros(nTargets)
    for iTarget in range(0, nTargets):

        t1 = time.clock()
        startTime =time.time()
        rf_density_regressor= RandomForestRegressor(n_estimators=numberOfTrees, n_jobs=use_cores, max_depth=treeDepth,
                                                     oob_score=True)

        # train model
        rf_density_regressor.fit(x_train_density, y_train_density.iloc[:,iTarget])
        print('Fitting is done [%.2f s]' % (time.time() - startTime))
        dt_rf_training = time.clock() - t1

        startTime =time.time()
        # evaluate model
        y_predicted_tmp = rf_density_regressor.predict(x_test_density)
        y_predicted[:,iTarget] = y_predicted_tmp

        #np.savetxt("y_predicted_tmp_{0}.csv".format(iTarget), y_predicted_tmp, delimiter=",")
        
        print('Prediction is done [%.2f s]' % (time.time() - startTime))

        # Evaluate performance
        score_training[iTarget] = rf_density_regressor.score(x_train_density, y_train_density.iloc[:, iTarget])
        score_test[iTarget] = rf_density_regressor.score(x_test_density, y_test_density.iloc[:, iTarget])
        score_oob[iTarget] = rf_density_regressor.oob_score_

    # np.savetxt("y_predicted.csv", y_predicted, delimiter=",")

    # standardization
    row_sums = y_predicted.sum(axis=1)
    y_predicted_normiert = y_predicted / row_sums[:, np.newaxis]

    combined = np.zeros((len(y_predicted_normiert), 6))
    combined[:, :3] = y_predicted_normiert
    combined[:, 3:] = y_test_density_np
    np.savetxt("y_predicted_norm.csv", combined, delimiter=",")

    log_file.write("\n * RANDOM FOREST RESULTS * \n")

    log_file.write("Number of heatmaps: %d\n" % data_frame_density.__len__())
    log_file.write("Size of heatmaps: [%d, %d]\n\n" % (data_frame_density.shape[0], data_frame_density.shape[1]))

    print(" ")
    print("**** RANDOM FOREST RESULTS ****")
    calc_and_print_errors_rf(y_test_density_np, y_predicted_normiert, log_file, score_training, score_test, score_oob)



    ###################### eval of the trees


    n = int(np.sqrt(len(rf_density_regressor.feature_importances_)))
    features_importance = np.reshape(rf_density_regressor.feature_importances_, [int(obs_area[3]/resolution),int(obs_area[2]/resolution)])

    # print(type(rf_density_regressor.feature_importances_))
    # print(np.size(rf_density_regressor.feature_importances_))
    # print(np.size(features_importance))

    plt.figure()
    plt.imshow(features_importance)
    plt.title("Feature importance")
    plt.colorbar()
    # plt.show()
    plt.savefig(log_file_name[0:len(log_file_name)-3] + 'pdf')

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

    return dt_rf_training



if __name__ == '__main__':

    test_size_percent = 0.2
    use_cores = 4

    directory = '../../data/output_preprocessed/'

    numberOfTrees = [10]
    treeDepths = [None] # as long as possible

    randomForest(test_size_percent, use_cores, directory, numberOfTrees, treeDepths)
          
          
          
