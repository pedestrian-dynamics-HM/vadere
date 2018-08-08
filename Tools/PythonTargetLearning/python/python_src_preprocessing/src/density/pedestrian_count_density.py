import math
import numpy as np

from src.io.density_writer import write_matrix_to_file

INDEX_TIME_STEP = 0
INDEX_PED_ID = 1
INDEX_POS_X = 2
INDEX_POS_Y = 3
INDEX_TARGET_ID = 4

# ----------------------------------------------------------------------------------------------------------------------
# Pedestrian count density
# ----------------------------------------------------------------------------------------------------------------------


# counts pedestrian density in units
# @param data chronologically sorted pedestrian data by time step
# @param size of measurement_field
def calculate_pedestrian_density(data, observation_area, resolution, output_root_directory, output_file_name, count):
    size_matrix = (int(observation_area[2] / resolution), int(observation_area[3] / resolution))
    tmp = np.array(data[4])
    for timestep in data:  # iterate over pedestrians
        matrix = np.zeros(size_matrix)  # new matrix for new time step
        for ped in timestep:
            add_pedestrian(ped, matrix, observation_area, resolution)

        write_matrix_to_file(matrix, output_root_directory, output_file_name, timestep[0][0], count)  # write matrix directly to file


# add density of ped depending on current possition
def add_pedestrian(ped, matrix, observation_area, resolution):
    x_pos = np.round((ped[INDEX_POS_X] - observation_area[0])/ resolution, 1)  # round to 1 numb after decimal point
    y_pos = np.round((ped[INDEX_POS_Y] - observation_area[1])/ resolution, 1)

    x_pos_frac, x_pos_int = math.modf(x_pos)
    y_pos_frac, y_pos_int = math.modf(y_pos)

    x_pos_int = int(x_pos_int)
    y_pos_int = int(y_pos_int)

    h = matrix.shape[1]

    pedVal = 1

    positions = []
    if x_pos_frac != 0 and y_pos_frac != 0:  # ped is only standing with in one field
        positions.append([x_pos_int, y_pos_int])
    else:  # border cases
        positions.append([x_pos_int, y_pos_int])  # at least current must added
        if x_pos_frac == 0:  # ped standing between two fields on x-axis
            if y_pos_frac == 0:  # ped standing between two fields on y-axis
                # check if coordinates are valid
                if x_pos_int > 0:
                    positions.append([x_pos_int - 1, y_pos_int])  # field to the left of ped
                    if y_pos_int > 0:
                        positions.append([x_pos_int, y_pos_int - 1])  # field below ped
                        positions.append([x_pos_int - 1, y_pos_int - 1])  # field diagonal from ped

            else:  # only between two field on x-axis
                if x_pos_int > 0:
                    positions.append([x_pos_int - 1, y_pos_int])
        else:  # only between two field on y-axis
            if y_pos_int > 0:
                positions.append([x_pos_int, y_pos_int - 1])

    for pos in positions:
        matrix[h - pos[1] - 1][pos[0]] = pedVal / len(positions)

