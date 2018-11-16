import time
import os
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from src.rf.error_calculation import  calc_and_print_errors



def load_data(directory, log_file):
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

    # split data in samples and response
    y_density = data_frame_density[data_frame_density.columns[-3:]]
    x_density = data_frame_density.iloc[:, 0:-3]

    return x_density, y_density

def apply_heuristic(directory, log_file, test_size_percent,obs_area, resolution):
    x_density, y_density = load_data(directory, log_file)
    # split data into training and test set of data
    x_train_density, x_test_density, y_train_density, y_test_density = train_test_split(x_density, y_density,
                                                                                        random_state=1,
                                                                                        test_size=test_size_percent)

    # apply heuristic to each matrix in the test set

    x_test_density_np = x_test_density._get_values
    y_test_density_np = y_test_density._get_values

    n_heatmaps = x_test_density_np.shape[0]
    dim_heatmaps_x = int(obs_area[2]/resolution)
    dim_heatmaps_y = int(obs_area[3]/resolution)

    error_euklid_all = -1*np.ones([n_heatmaps,1])
    estimate3_all =    -1*np.ones([n_heatmaps,3])

    for i in range(0, n_heatmaps-1):
        snapshot = np.reshape(x_test_density_np[i], [dim_heatmaps_y, dim_heatmaps_x])

        # # Method 1
        # part_left = int(np.round(dim_heatmaps_x/3))
        # part_straight = int(np.round(dim_heatmaps_x/3))
        # part_right = int(dim_heatmaps_x - part_straight-part_left)
        #
        # vals_left = snapshot[:,0:part_left]
        # vals_straight = snapshot[:,part_left:part_left+part_straight]
        # vals_right = snapshot[:,part_left+part_straight:dim_heatmaps_x]
        #
        # total = np.sum(vals_left)*part_left + np.sum(vals_straight)*part_straight + np.sum(vals_right)*part_right
        # part_left = np.sum(vals_left)*part_left / total
        # part_straight = np.sum(vals_straight)*part_straight / total
        # part_right = np.sum(vals_right)*part_right / total
        #
        # print('First estimate: [%d %d %d]' % (part_left*100, part_straight*100, part_right*100))
        #
        # # Method 2
        #
        # part_left2 = np.sum(vals_left) / np.sum(snapshot)
        # part_straight2 = np.sum(vals_straight) / np.sum(snapshot)
        # part_right2 = np.sum(vals_right) / np.sum(snapshot)
        #
        # print('Second estimate: [%d %d %d]' % (part_left2*100, part_straight2*100, part_right2*100))

        # Method #3

        if np.mod(dim_heatmaps_x,3) == 0: # multiple of 3
            part_left = int(dim_heatmaps_x / 3)
            part_straight = part_left
            part_right = part_left

            vals_left = snapshot[:, 0:part_left]
            vals_straight = snapshot[:, part_left:2*part_left]
            vals_right = snapshot[:, 2*part_left:dim_heatmaps_x]

            total = np.sum(vals_left) * part_left + np.sum(vals_straight) * part_straight + np.sum(
                vals_right) * part_right
            part_left = np.sum(vals_left) * part_left / total
            part_straight = np.sum(vals_straight) * part_straight / total
            part_right = np.sum(vals_right) * part_right / total

        else:

            idx_left_straight = int(np.floor(dim_heatmaps_x / 3))
            idx_straight_right = int(dim_heatmaps_x - np.ceil(dim_heatmaps_x / 3))

            # whole parts
            vals_left3 = np.sum(snapshot[:, 0:idx_left_straight ])
            vals_straight3 = np.sum(snapshot[:, idx_left_straight+1:idx_straight_right])
            vals_right3  = np.sum(snapshot[:, idx_straight_right +1 : dim_heatmaps_x])

            factor = np.mod(dim_heatmaps_x,3)/3

            vals_left3 = vals_left3 + np.sum(snapshot[:,idx_left_straight]) * factor
            vals_straight3 = vals_straight3 + np.sum(snapshot[:,idx_left_straight]) * (1-factor) + np.sum(snapshot[:,idx_straight_right]) * (1-factor)
            vals_right3 = vals_right3 + np.sum(snapshot[:,idx_straight_right]) * factor

            # Make sure Method 3 works
            assert (vals_left3 + vals_straight3 + vals_right3 - np.sum(snapshot) < 1e-10)

            estimate3 = np.array([vals_left3/np.sum(snapshot), vals_straight3/np.sum(snapshot), vals_right3/np.sum(snapshot)])


        # Save results
        error_euklid = np.sqrt(np.sum(np.square(y_test_density_np[i] - estimate3)))
        error_euklid_all[i,:] = error_euklid
        estimate3_all[i,:] = estimate3

        # Error in each heatmap
        # print(" ")
        # print('Actual distribution: [%.4f %.4f %.4f]' % (y_test_density_np[i][0], y_test_density_np[i][1], y_test_density_np[i][2]))
        # print('Estimate:            [%.4f %.4f %.4f]' % (        estimate3[0], estimate3[1],        estimate3[2] ))

        # print(" ")
        # print(" * RESULTS * \n")
        # print('Mean Euclidean Error: %.4f' % error_euklid)
        # print('Mean Euclidean Error: %.2f%%' % (error_euklid / np.sqrt(2) * 100))


    # evaluate all

    print("   * HEURISTIC RESULTS * ")
    print("    (Mean over all test snapshots)")



    log_file.write("\n * HEURISTIC RESULTS  * \n")
    calc_and_print_errors(y_test_density_np, estimate3_all, log_file)

    '''
    # Euclidean error
    euklid_error2 = np.sqrt(np.sum(np.square(estimate3_all - y_test_density_np), 1))
    euklid_error_mean2 = np.mean(euklid_error2)
    euklid_error_mean_percent2 = euklid_error_mean2 / np.sqrt(2) * 100

    # RMSE (per Direction)
    rmse_per_dir = np.sqrt(np.mean(np.square(estimate3_all - y_test_density_np),axis=0))
    ## Print results
    print(" ")
    print("Mean Euclidean Error (Test data): %f" % euklid_error_mean2)
    print("Mean Euclidean Error (Test data): %.2f%%" % euklid_error_mean_percent2)
    print(" ")

    print("RMSE (Test data) Per Direction: [%.4f %.4f %.4f]" % (rmse_per_dir[0], rmse_per_dir[1], rmse_per_dir[2]))
    print("RMSE (Test data) Per Direction: [%.2f%% %.2f%% %.2f%%]" % (rmse_per_dir[0]*100, rmse_per_dir[1]*100, rmse_per_dir[2]*100))

    print(" ")
    print("Mean of Euclidean Errors: %f " % np.mean(error_euklid_all))
    print("Mean of Euclidean Errors: %.2f%%" % (np.mean(error_euklid_all)/np.sqrt(2)*100))

    print(" ")
    print("Std  of Euclidean Errors: %f " % np.std(error_euklid_all))
    print("Std  of Euclidean Errors: %.2f%%" % (np.std(error_euklid_all)/np.sqrt(2)*100))


    # Write results to log file
    log_file.write("\n * HEURISTIC RESULTS  * \n")

    log_file.write("RMSE (Test data) Per Direction: [%f %f %f]\n" % (rmse_per_dir[0], rmse_per_dir[1], rmse_per_dir[2]))
    log_file.write("RMSE (Test data) Per Direction: [%.2f%% %.2f%% %.2f%%] \n\n" % (rmse_per_dir[0]*100, rmse_per_dir[1]*100, rmse_per_dir[2]*100))

    log_file.write("Mean of Euclidean Errors: %f \n" % np.mean(error_euklid_all))
    log_file.write("Mean of Euclidean Errors: %.2f%%\n" % (np.mean(error_euklid_all)/np.sqrt(2)*100))

    log_file.write("Std  of Euclidean Errors: %f \n" % np.std(error_euklid_all))
    log_file.write("Std  of Euclidean Errors: %.2f%%\n" % (np.std(error_euklid_all)/np.sqrt(2)*100))


    print(" ")
    '''



