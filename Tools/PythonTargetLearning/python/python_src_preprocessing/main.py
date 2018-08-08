#############################################################################################
##  Data preprocessing and training a random forest
#############################################################################################

import os
import numpy as np
from time import clock
import shutil
import pandas as pd

import sys
sys.path.append('../')
from python_src_preprocessing.src.rf.randomForestRegression_marion_concise import randomForest
from python_src_preprocessing.src.io.density_writer import write_to_csv, get_output_file_name
from python_src_preprocessing.src.io.trajectory_reader import read_trajectory_file \
    , get_all_trajectory_files \
    , get_all_overlap_files \
    , convert_data \
    , extract_observation_area \
    , sort_chronological \
    , extract_period_from_to \
    , calculate_pedestrian_target_distribution
from python_src_preprocessing.src.density.gaussian import calculate_density_timeseries
from python_src_preprocessing.src.density.pedestrian_count_density import calculate_pedestrian_density
from python_src_preprocessing.src.io.attribute_file_generator import generate_attributes_file

# TODO: conversion of timesteps to seconds!

# ----------------------------------------------------------------------------------------------------------------------
VERSION = 1.0
# ----------------------------------------------------------------------------------------------------------------------

# directory to read input files from
#INPUT_ROOT_DIRECTORY = os.path.join('C:/Studium/BA/DatenGruppe/ModSim17-data-generation-filters/input/')
#INPUT_ROOT_DIRECTORY = os.path.join('C:/Studium/BA/vadereProjects/output')
INPUT_ROOT_DIRECTORY = os.path.join('C:/Studium/BA/datensets/2018-08-07-new-vadere-version')


# directory to write output files to
OUTPUT_ROOT_DIRECTORY = os.path.join('C:/Studium/BA/Vadere/vadere/Tools/PythonTargetLearning/output/')
OBSERVATION_AREA = [7,23,10,10]#[20, 5, 10, 10] #[25, 5, 10, 10]  # select data from observed area, [offset_x, offset_y, width, height]
OBSERVATION_AREA = [20, 5, 10, 10] #[25, 5, 10, 10]  # select data from observed area, [offset_x, offset_y, width, height]


OBSERVATION_AREA = [19.5, 55.0, 6, 10] #langer Gang/assymetrisch
#OBSERVATION_AREA = [7, 33, 10, 10] # eventuell für brücke


TIME_STEP_BOUNDS = (30, 0)  # cut off number of timesteps from start and end time
RESOLUTION = 0.5  # resolution for density calculations
SIGMA = 0.7  # constant for gaussian density function, see `gaussian.py`
GAUSS_DENSITY_BOUNDS = (2,2)  # side length of quadratic area for gaussian density
FRAMERATE = 1



def data_preprocessing():
    overlap_files = get_all_overlap_files(INPUT_ROOT_DIRECTORY)
    
    number_of_overlaps = 0
    for filename in overlap_files:
        file = pd.read_csv(filename, sep = ' ')
        overlaps =  file["overlaps"].tolist()
        
        for i, value in enumerate(overlaps):
            if value != 0:
                '''raise ValueError'''
                number_of_overlaps += 1
                #print('Overlaps in file' ''', filename''', 'is ', value)
    print(number_of_overlaps, 'Overlaps')
    
    trajectory_files = get_all_trajectory_files(INPUT_ROOT_DIRECTORY)
    number_of_files = len(trajectory_files)

    shutil.copy(INPUT_ROOT_DIRECTORY + '/generatedDistributions.txt', OUTPUT_ROOT_DIRECTORY + '/generatedDistributions.txt')

    for i in range(0, number_of_files):  # process each file successively

        print("Number of file: %d" %i)
        print("Processing %s" % trajectory_files[i])
        # determine distribution of pedestrians on targets within measurement area in relevant time steps
        data_period, pedestrian_target_distribution, global_distribution, timesteps = process_data_file(trajectory_files[i])

        if (data_period): # not 'None'

            # generate file name through pedestrian target distribution
            output_file_name = get_output_file_name(global_distribution)  # filename with global dist
            #print(output_file_name)
            with open(OUTPUT_ROOT_DIRECTORY +'\\'+ output_file_name +"_" +str(i) + '.csv', mode='w') as file: # mode w: existing file is deleted!
                # calculate Gaussian density
                calculate_density_timeseries(data_period, OBSERVATION_AREA, \
                                             RESOLUTION, GAUSS_DENSITY_BOUNDS, SIGMA, \
                                             pedestrian_target_distribution, file)

            print("Done: ", str(np.round(((i+1) / number_of_files) * 100,0)), " %")
            #print(output_file_name + str(i), " = ", trajectory_files[i])

            # Datatype, script version tag, OBSERVATION_AREA,
            # TIME_STEP_BOUNDS, RESOLUTION, SIGMA, GAUSS_DENSITY_BOUNDS, scenarios used
            generate_attributes_file(OUTPUT_ROOT_DIRECTORY,["gaussian density",str(VERSION),str(OBSERVATION_AREA), str(TIME_STEP_BOUNDS), \
                                                    str(RESOLUTION), str(SIGMA), str(GAUSS_DENSITY_BOUNDS),str(FRAMERATE), str(trajectory_files).replace("input\\"," ")])
        else:
            print("** Trajectory file is empty: %s" % trajectory_files[i])


    # pedestrian_count_main()



def process_data_file(file):
    # read single trajectory file
    data_raw = read_trajectory_file(file)

    if len(data_raw) > 0:

        #  convert to numeric data
        data_numeric = convert_data(data_raw)

        # extract data from a specified observation area
        data_observation = extract_observation_area(data_numeric, OBSERVATION_AREA)

        # sort time steps chronological & group data sets with identical time step
        data_chronological = sort_chronological(data_observation)

        # extract observation period
        data_period = extract_period_from_to(data_chronological, TIME_STEP_BOUNDS, data_numeric)

        # use only every snapshot in the defined FRAMERATE
        data_reduced = []
        for time in data_period:
            if time[0][0] % FRAMERATE == 0:
                data_reduced.append(time)


        # calculate pedestrian target distribution
        pedestrian_target_distribution, global_distribution, timesteps = \
            calculate_pedestrian_target_distribution(data_reduced)  # use data before it is sorted!

        return data_reduced, pedestrian_target_distribution, global_distribution, timesteps
    else:
        return None, None, None, None




def pedestrian_count_main(): # unused!
    trajectory_files = get_all_trajectory_files(INPUT_ROOT_DIRECTORY)
    number_of_files = len(trajectory_files)
    file_name = ""
    for i in range(0, number_of_files):

        with open(OUTPUT_ROOT_DIRECTORY + file_name, mode='a') as file:
            data_period, pedestrian_target_distribution = process_data_file(trajectory_files[i])
            # calculate pedestrian target distribution
            output_file_name = get_output_file_name(pedestrian_target_distribution, name="count_density_")
            # calculate density
            calculate_pedestrian_density(data_period, OBSERVATION_AREA, 1, OUTPUT_ROOT_DIRECTORY, output_file_name, i)




### Call main!

if __name__ == '__main__':
    print("**** PRE-PROCESSING OF DATA      ****")
    t1 = clock()

    print('hallo')

    data_preprocessing()
    dt = clock() - t1
    print("Calculation time %d [s]" % dt)
    print("**** PRE-PROCESSING OF DATA DONE ****")


    ## Parameters for random forest
    test_size_percent = 0.2
    use_cores = 4
    numberOfTrees = [100]
    treeDepths = [None] # as long as possible

    print(" ")
    print("**** RANDOM FOREST       ****")
    t1 = clock()
    randomForest(test_size_percent, use_cores, OUTPUT_ROOT_DIRECTORY, numberOfTrees, treeDepths, OBSERVATION_AREA, RESOLUTION)
    dt = clock() - t1
    print("Time for random forest: %d [s]" %dt)
    print("**** RANDOM FOREST DONE  ****")
