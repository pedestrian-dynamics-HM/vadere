#############################################################################################
# Data preprocessing and training a random forest
#############################################################################################

import os
import time
import density
import heuristic.t_junction as heuristic
import utils.reader as reader
import utils.writer as writer
import utils.generator as generator
import numpy as np
from datetime import datetime
from rf.randomForestRegression_marion_concise_multiple_forests import randomForest

# ----------------------------------------------------------------------------------------------------------------------
VERSION = 1.0
# ----------------------------------------------------------------------------------------------------------------------

BOOL_preprocessing = False
BOOL_heuristic = True
BOOL_rf = True

# directory to read input files from
INPUT_ROOT_DIRECTORY = os.path.join('C:/Users/Luca/Documents/PED_Can_we_learn_where_people_go/output_UNIT_Potential/')

# directory to write output files to
OUTPUT_ROOT_DIRECTORY = os.path.join('C:/Users/Luca/Documents/PED_Can_we_learn_where_people_go/output_preprocessed/')

# directory to write result files to
RESULT_ROOT_DIRECTORY = os.path.join('C:/Users/Luca/Documents/PED_Can_we_learn_where_people_go/myresults/')


OUTPUT_ROOT_DIRECTORY = os.path.join('../../../notebooks/juelich/output_01/')
RESULT_ROOT_DIRECTORY = os.path.join('../../../notebooks/juelich/results_01/')

INPUT_ROOT_DIRECTORY = os.path.join('I:/projects/vadere/Tools/PythonTargetLearning/notebooks/check/outputs/')
OUTPUT_ROOT_DIRECTORY = os.path.join('I:/projects/vadere/Tools/PythonTargetLearning/notebooks/check/csvs/obstacle/')
RESULT_ROOT_DIRECTORY = os.path.join('I:/projects/vadere/Tools/PythonTargetLearning/notebooks/check/results/obstacle/')

# OBSERVATION_AREA select data from observed area, [offset_x, offset_y, width, height]
# [7,23,5,5]
# [20, 5, 10, 10]
# [25, 5, 10, 10]
# OBSERVATION_AREA = [20, 10, 10, 10]
# OBSERVATION_AREA = [13.50, 7.25, 2.4, 1]
OBSERVATION_AREA = [18.50, 6.7, 2.4, 1]
# OBSERVATION_AREA = [-2.4, 3, 2.4, 1]

TIME_STEP_BOUNDS = (30, 0)  # cut off number of timesteps from start and end time
RESOLUTION = 0.1  # 0.5  # resolution for density calculations
SIGMA = 0.7  # constant for gaussian density function, see `gaussian.py`
GAUSS_DENSITY_BOUND = 2  # side length of quadratic area for gaussian density
FRAMERATE = 20
SIM_TIME_STEP_LENGTH = 0.4  # unused right now

# Parameters for random forest
test_size_percent = 0.2
use_cores = 4
# directory = '../../data/output_preprocessed/'
NTREES = 50
number_of_targets = 3  # default
tree_depth = None  # as long as possible


# LEFT: Target ID 1
# STRAIGHT: Target ID 2
# RIGHT: Target ID 3

def start_logging():
	global INPUT_ROOT_DIRECTORY, OUTPUT_ROOT_DIRECTORY, OBSERVATION_AREA, TIME_STEP_BOUNDS, RESOLUTION, SIGMA, GAUSS_DENSITY_BOUND, FRAMERATE, NTREES, BOOL_preprocessing, BOOL_heuristic, BOOL_rf

	now = datetime.now()
	log_file_name = ("Results_%d-%d-%d_%d-%d-%d.txt" % (now.year, now.month, now.day, now.hour, now.minute, now.second))
	log_file = open(os.path.join(RESULT_ROOT_DIRECTORY, log_file_name), 'w')

	log_file.write("Results of Python Processing of Data \n")
	log_file.write("\n * PARAMETER SET * \n")
	if BOOL_preprocessing:
		log_file.write("Input data directory: %s \n" % INPUT_ROOT_DIRECTORY)
	else:
		log_file.write("Preprocessed data directory: %s \n" % OUTPUT_ROOT_DIRECTORY)
		# import values from attributes file

		if os.path.isfile(OUTPUT_ROOT_DIRECTORY + "attributes.txt"):
			attributes_file = open(OUTPUT_ROOT_DIRECTORY + "attributes.txt")
			iline = 0
			while iline < 27:
				tline = attributes_file.readline()  # observation area
				iline = iline + 1
				if iline == 10:  # observation area
					val1, val2, val3, val4 = tline[1:tline.__len__() - 2].split(",")
					OBSERVATION_AREA = [float(val1), float(val2), float(val3), float(val4)]
				elif iline == 13:  # time step bounds
					val1, val2 = tline[1:tline.__len__() - 2].split(",")
					TIME_STEP_BOUNDS = (float(val1), float(val2))
				elif iline == 16:  # resolution
					RESOLUTION = float(tline)
				elif iline == 19:  # sigma
					SIGMA = float(tline)
				elif iline == 22:  # gauss density bounds
					GAUSS_DENSITY_BOUND = float(tline)
				elif iline == 25:  # frame rate
					FRAMERATE = float(tline)

	log_file.write("Observation area / Camera cutout [x,y,width,height]: [%.2f %.2f %.2f %.2f] m \n" % (
		OBSERVATION_AREA[0], OBSERVATION_AREA[1], OBSERVATION_AREA[2], OBSERVATION_AREA[3]))
	log_file.write(
		"Time steps bounds [time steps]: [start + %d, end - %d] \n" % (TIME_STEP_BOUNDS[0], TIME_STEP_BOUNDS[1]))
	log_file.write("Frame rate [time steps]: %d \n" % FRAMERATE)
	log_file.write("Bound for Gaussian density: %d m \n" % GAUSS_DENSITY_BOUND)
	log_file.write("Resolution: %.2f m \n" % RESOLUTION)

	log_file.write("Percentage of test data: %f \n" % test_size_percent)
	log_file.write("Number of trees per forest: %d \n" % + NTREES)
	#    log_file.write("Tree depth: %s" % treeDepth)

	log_file.write("Number of targets: %d \n" % number_of_targets)
	log_file.flush()

	print("Log file: %s" % log_file_name)

	return log_file, log_file_name


def data_preprocessing(log_file):
	global INPUT_ROOT_DIRECTORY, OUTPUT_ROOT_DIRECTORY, OBSERVATION_AREA, TIME_STEP_BOUNDS, RESOLUTION, SIGMA, GAUSS_DENSITY_BOUND, FRAMERATE

	# clear output directory
	for the_file in os.listdir(OUTPUT_ROOT_DIRECTORY):
		file_path = os.path.join(OUTPUT_ROOT_DIRECTORY, the_file)
		try:
			if os.path.isfile(file_path):
				os.unlink(file_path)
				# elif os.path.isdir(file_path): shutil.rmtree(file_path)
		except Exception as e:
			print(e)

	# read input files (trajectories)
	trajectory_files = reader.get_all_trajectory_files(INPUT_ROOT_DIRECTORY)
	number_of_files = len(trajectory_files)

	for i in range(0, number_of_files):  # process each file successively

		print("Number of file: %d" % i)
		print("Processing %s" % trajectory_files[i])
		# determine distribution of pedestrians on targets within measurement area in relevant time steps
		data_period, pedestrian_target_distribution, global_distribution, timesteps = process_data_file(
			trajectory_files[i], OBSERVATION_AREA, FRAMERATE, TIME_STEP_BOUNDS)

		if data_period:  # not 'None'

			# generate file name through pedestrian target distribution
			output_file_name = writer.get_output_file_name(global_distribution)  # filename with global dist
			print(output_file_name)

			
			# mode w: existing file is deleted!
			with open(OUTPUT_ROOT_DIRECTORY + '/' + output_file_name + "_" + str(i) + '.csv', mode='w') as file:

				# use formula to calculate Gaussian density
				# t1 = clock()
				# calculate_density_timeseries_formula(data_period, obs_area,
				#                             resolution, SIGMA, pedestrian_target_distribution, file)
				# dt_formula = clock() - t1
				# print("Density calculation with formula took %.2f s" % dt_formula)

				# calculate Gaussian density
				t1 = time.clock()
				density.calculate_density_timeseries(data_period, OBSERVATION_AREA,
													 RESOLUTION, GAUSS_DENSITY_BOUND, SIGMA,
													 pedestrian_target_distribution, file, bool_exact_position=False)
				dt_bounds = time.clock() - t1
				print("Density calculation with bounds (rough position) took %.2f s" % dt_bounds)

				# calculate Gaussian density
				# t1 = clock()
				# calculate_density_timeseries(data_period, obs_area,
				#                             resolution, GAUSS_DENSITY_BOUND, SIGMA,
				#                             pedestrian_target_distribution, file, bool_exact_position=True)
				# dt_bounds = clock() - t1
				# print("Density calculation with bounds took %.2f s" % dt_bounds)

			print("Done: ", str(np.round(((i + 1) / number_of_files) * 100, 0)), "%")
			# print(output_file_name + str(i), " = ", trajectory_files[i])

			# Datatype, script version tag, obs_area,
			# TIME_STEP_BOUNDS, resolution, SIGMA, GAUSS_DENSITY_BOUND, scenarios used
			generator.generate_attributes_file(OUTPUT_ROOT_DIRECTORY,
											   ["gaussian density", str(VERSION), str(OBSERVATION_AREA),
												str(TIME_STEP_BOUNDS),
												str(RESOLUTION), str(SIGMA), str(GAUSS_DENSITY_BOUND), str(FRAMERATE),
												str(trajectory_files).replace("input\\", " ")])
		else:
			print("** Trajectory file is empty: %s" % trajectory_files[i])

	# Logging
	log_file.write("\n * Pre-processing of files * \n")
	log_file.write("Number of files: %d \n" % number_of_files)

	log_file.flush()

	# log_file.write("Total number of time steps: %d" % )
	# pedestrian_count_main()


def process_data_file(file, obs_area, framerate, time_step_bounds):
	# read single trajectory file
	data_raw = reader.read_trajectory_file(file)

	if len(data_raw) > 0:

		#  convert to numeric data
		data_numeric = reader.convert_data(data_raw)

		# extract data from a specified observation area
		data_observation = reader.extract_observation_area(data_numeric, obs_area)

		# sort time steps chronological & group data sets with identical time step
		data_chronological = reader.sort_chronological(data_observation)

		# extract observation period
		data_period = reader.extract_period_from_to(data_chronological, time_step_bounds, data_numeric)

		# use only every snapshot in the defined framerate
		data_reduced = []
		for timestep in data_period:
			if timestep[0][0] % framerate == 0:
				data_reduced.append(timestep)

		# calculate pedestrian target distribution
		pedestrian_target_distribution, global_distribution, timesteps = \
			reader.calculate_pedestrian_target_distribution(data_reduced)  # use data before it is sorted!

		return data_reduced, pedestrian_target_distribution, global_distribution, timesteps
	else:
		return None, None, None, None


def pedestrian_count_main(obs_area, framerate, time_step_bounds):
	trajectory_files = reader.get_all_trajectory_files(INPUT_ROOT_DIRECTORY)
	number_of_files = len(trajectory_files)
	file_name = ""
	for i in range(0, number_of_files):
		with open(OUTPUT_ROOT_DIRECTORY + file_name, mode='a') as file:
			data_period, pedestrian_target_distribution = process_data_file(trajectory_files[i], obs_area, framerate,
																			time_step_bounds)
			# calculate pedestrian target distribution
			output_file_name = writer.get_output_file_name(pedestrian_target_distribution, name="count_density_")
			# calculate density
			density.calculate_pedestrian_density(data_period, obs_area, 1, OUTPUT_ROOT_DIRECTORY, output_file_name, i)


def run_main(observation_area, resolution, framerate, number_of_trees):
	global OBSERVATION_AREA, RESOLUTION, FRAMERATE, NTREES

	if not (observation_area is None):
		OBSERVATION_AREA = observation_area
	if not (resolution is None):
		RESOLUTION = resolution
	if not (framerate is None):
		FRAMERATE = framerate
	if not (number_of_trees is None):
		NTREES = number_of_trees

	# Logging
	log_file, log_file_name = start_logging()

	# Pre processing of data
	if BOOL_preprocessing:
		print("**** PRE-PROCESSING OF DATA      ****")
		t1 = time.clock()
		data_preprocessing(log_file)
		dt_preprocessing = time.clock() - t1
		print("Calculation time %.2f [s]" % dt_preprocessing)
		print("**** PRE-PROCESSING OF DATA DONE ****")

	# HEURISTIC
	if BOOL_heuristic:
		print("**** HEURISTIC      ****")
		t2 = time.clock()
		heuristic.apply_heuristic(OUTPUT_ROOT_DIRECTORY, log_file, test_size_percent, OBSERVATION_AREA, RESOLUTION)
		dt_heuristic = time.clock() - t2
		print("Calculation time %.2f [s]" % dt_heuristic)
		print("**** HEURISTIC DONE ****")
	# RANDOM FOREST

	if BOOL_rf:
		print(" ")
		print("**** RANDOM FOREST       ****")
		t3 = time.perf_counter()
		dt_rf_training = randomForest(
			test_size_percent,
			use_cores,
			OUTPUT_ROOT_DIRECTORY,
			RESULT_ROOT_DIRECTORY,
			number_of_trees,
			tree_depth,
			number_of_targets,
			log_file,
			log_file_name,
			observation_area,
			resolution
		)
		dt_rf = time.perf_counter() - t3
		print("Time for random forest %.2f [s]" % dt_rf)
		print("**** RANDOM FOREST DONE  ****")

	# Write results to log file
	log_file.write("\n * COMPUTATION TIME *\n")
	if BOOL_preprocessing:
		log_file.write("Time for pre-processing: %.2f s\n" % dt_preprocessing)
	else:
		log_file.write("No pre-processing performed!\n")

	if BOOL_heuristic:
		log_file.write("Time for heuristic     : %.2f s\n" % dt_heuristic)
	else:
		log_file.write("No heuristic applied!\n")

	if BOOL_rf:
		log_file.write("Time for random forest : %.2f s\n" % dt_rf)
		log_file.write("Time for training RF: %.2f\n" % dt_rf_training)

	log_file.flush()
	log_file.close()


# Call main!

if __name__ == '__main__':
	run_main(OBSERVATION_AREA, RESOLUTION, FRAMERATE, NTREES)
