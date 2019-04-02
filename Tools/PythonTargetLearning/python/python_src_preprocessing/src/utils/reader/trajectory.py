import csv
from glob import glob
import numpy as np
import pandas as pd

################################################################################
# run with `python main_rf.py`                                                    #
#                                                                              #
# vadere trajectories file has following format:                               #
# timeStep {int} | pedestrianId {int} | x {float} | y {float} | targetId {int} #
################################################################################

# before processing trajectories file check attribute's column position
INDEX_TIME_STEP = 0
INDEX_PED_ID = 1
INDEX_POS_X = 2
INDEX_POS_Y = 3
INDEX_TARGET_ID = 4


# Helper function to get all input file names from given root directory.
# 
# @param path: root directory to search from
def get_all_trajectory_files(root_dir):
    files = glob(root_dir + '/**/*.trajectories')
    return files


def read_trajectory_file(path, fps=None):
    frame = pd.read_csv(path, sep=' ', header=0)

    if fps is not None:
        d = 2

    return frame

'''
def read_trajectory_file(path):
    #print(path)
    file = open(path, newline='\n')
    file_reader = csv.reader(file, delimiter=' ')
    scenario = []
    for row in file_reader:
        scenario.append(row)
    return scenario[1:] # remove head row with labels
'''

def convert_data(data):
    def convert_row(row):
        # cast each col element
        time_step = int(row[INDEX_TIME_STEP])
        pedestrian_id = int(row[INDEX_PED_ID])
        pos_x = float(row[INDEX_POS_X])
        pos_y = float(row[INDEX_POS_Y])
        target_id = int(row[INDEX_TARGET_ID])
        return [time_step, pedestrian_id, pos_x, pos_y, target_id]

    return list(map(convert_row, data))


def extract_observation_area(data, area):
    x_meas = area[0]
    y_meas = area[1]
    width = area[2]
    height = area[3]
    row_obs_area = ([row for row in data if (x_meas <= row[INDEX_POS_X] <= (x_meas + width)) and
             (y_meas <= row[INDEX_POS_Y] <= (y_meas + height))])
    if len(row_obs_area) == 0: # empty
        raise ValueError('** Check observation area! No pedestrians have stepped in the observation area**')

    return row_obs_area


# number of targets hardcoded, currently 3
# target ids hardcoded
# also calculate total distribution
def calculate_pedestrian_target_distribution(data):
    current_dist = []
    timesteps = []
    for timestep in data:
        timesteps.append(timestep[0][0])
        target_id_counts = [0, 0, 0]
        for row in timestep:
            if row[INDEX_TARGET_ID] == 1: # LEFT
                target_id_counts[0] += 1
            elif row[INDEX_TARGET_ID] == 2: # STRAIGHT
                target_id_counts[1] += 1
            elif row[INDEX_TARGET_ID]: # RIGHT
                target_id_counts[2] += 1

        n_pedestrians_timestep = len(timestep)  # No. of pedestrians in simulation in the measurement area
        target_dist = [(x / n_pedestrians_timestep) for x in target_id_counts]

        # convert absolute number to percentages
        current_dist.append(target_dist)


    length = len(current_dist)
    tmp = np.array(current_dist)
    total_dist = [np.sum(tmp[:,0]) / length, np.sum(tmp[:,1]) / length, np.sum(tmp[:,2]) / length]

    return current_dist, total_dist, timesteps


def sort_chronological(data):

    print("Len(trajectory file data): %d" % len(data))
    data_sorted = sorted(data, key=lambda row:row[INDEX_TIME_STEP])

    # compare data to data_sorted
    if data_sorted.__eq__(data):
        print("Sorting of data is not necessary.")
    else:
        print("Sorting of data is necessary.")

    # find rows that have the same timestep
    current_time = data_sorted[0][INDEX_TIME_STEP]
    data_chron = []
    rows_equal_time = []
    for row in data_sorted:
        if row[INDEX_TIME_STEP] == current_time:
            rows_equal_time.append(row)
        else:
            data_chron.append(rows_equal_time)
            rows_equal_time = []
            rows_equal_time.append(row)
            current_time += 1

    return data_chron

def extract_period_from_to(data_chron, time_step_bounds, data_numerical):

    start_time_step = time_step_bounds[0]
    print("Start time step %d" % start_time_step)

    t_max_chron = data_chron[-1][INDEX_TIME_STEP]
    t_max = data_numerical[-1][INDEX_TIME_STEP]


    if t_max_chron[0] < t_max- time_step_bounds[1]:
           stop_time_step = t_max_chron[0]
    else:
        stop_time_step = t_max - time_step_bounds[1]

    print("Stop time step %d" % stop_time_step)

    data_chron_bounds = []
    for time_step in data_chron:
        cur_time = time_step[0][INDEX_TIME_STEP]
        if start_time_step <= cur_time <= stop_time_step:
            data_chron_bounds.append(time_step)

    return data_chron_bounds
