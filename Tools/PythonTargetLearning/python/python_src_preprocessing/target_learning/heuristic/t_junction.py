import time
import os
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from rf.error_calculation import  calc_and_print_errors



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
    y_density = data_frame_density[data_frame_density.columns[-3:-1]]
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
    estimate_all =    -1*np.ones([n_heatmaps,2])

    for i in range(0, n_heatmaps-1):
        snapshot = np.reshape(x_test_density_np[i], [dim_heatmaps_y, dim_heatmaps_x])

        part_left = int(dim_heatmaps_x / 2)
        part_right = part_left

        vals_left = snapshot[:, 0:part_left]
        vals_right = snapshot[:, part_left:dim_heatmaps_x]

        left = np.sum(vals_left) * part_left
        right = np.sum(vals_right) * part_right

        total = left + right

        part_left = left / total
        part_right = right / total

        estimate = np.array([part_right, part_left])

        # Save results
        error_euklid = np.sqrt(np.sum(np.square(y_test_density_np[i] - estimate)))
        error_euklid_all[i,:] = error_euklid
        estimate_all[i,:] = estimate

    # evaluate all
    print("   * HEURISTIC RESULTS * ")
    print("    (Mean over all test snapshots)")

    log_file.write("\n * HEURISTIC RESULTS  * \n")
    calc_and_print_errors(y_test_density_np, estimate_all, log_file)
