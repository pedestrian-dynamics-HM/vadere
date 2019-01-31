#!/usr/bin/env python3
#  TODO: """ << INCLUDE DOCSTRING (one-line or multi-line) >> """

# include imports after here:

import os
import json
import pandas as pd
import numpy as np
from os import path
import suq_controller.scenario_handling as scenario_handling
import datetime

import suq_controller.simulation as simulation

# --------------------------------------------------
# Change default values
__authors__ = "Marion Gödel"
__credits__ = ["N/A"] # insert names of people who reported bugs, made suggestions but didn't write code
# --------------------------------------------------



# parameter: uncertain parameter
 # parameter grid (1d): values for uncertain parameter
def run(scenario_file_name, uncertain_parameter, parameter_grid, qoi, output_scenario_path):

    # Only for scalar QoI
    qoi_output = np.zeros(len(parameter_grid))

    """ open scenario"""
    scenario = json.load(open(scenario_file_name))
    tmp = str.split(scenario_file_name, ".")
    scenario_name = tmp[0]
    # scenario_handling.save_scenario(scenario, output_scenario_path, scenario_file_name)


    """ add relevant output processors to the scenario"""
    scenario_handling.add_processor_for_qoi(scenario, qoi)



    count = 0
    for node in parameter_grid:
        output_scenario_name = scenario_name+"_run_"+str(count)

        """ vary uncertain parameter """
        scenario = manipulate_scenario(scenario, output_scenario_name, node, uncertain_parameter)

        """ save scenario """
        scenario_output_path, scenario_path_file = save_scenario(scenario, output_scenario_path, output_scenario_name, scenario_file_name)

        """ run simulation """
        simulation.run_vadere(scenario_path_file, scenario_output_path)

        """ extract quantity of interest """
        path_trajectories_file = scenario_output_path+"//postvis.trajectories"
        print(path.abspath(path_trajectories_file))
        qoi_output[count] = extract_qoi(scenario, path.abspath(path_trajectories_file)) # ,qoi)
        count = count +1

    return qoi_output


def manipulate_scenario(scenario, output_scenario_name, node, uncertain_parameter):
    source_id = 0  # id of source to be manipulated

    if str.__eq__(uncertain_parameter, "spawnNumber"):
        scenario = scenario_handling.set_spawn_number(scenario, int(node))
    else:
        raise Exception("Uncertain parameter is not yet supported")

    scenario = scenario_handling.set_name(scenario, output_scenario_name)
    return scenario


def extract_qoi(scenario, path_trajectories_file):

    """ read the output file """
    df_a = pd.read_csv(path_trajectories_file, sep=' ')
    last_timeStep = df_a[df_a["state"] == "d"].tail(1)["timeStep"]
    last_timeStep = float(str(last_timeStep).split()[1])

    """ calc evacuation time """
    time_step = scenario_handling.get_timestep(scenario)
    evacuation_time = last_timeStep * time_step

    return evacuation_time

def save_scenario(scenario, output_scenario_path, output_scenario_folder, scenario_file_name):
    str_now = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S.%f")
    output_path = output_scenario_path + "/" + output_scenario_folder + "/" + str_now
    os.makedirs(output_path)
    output_file = output_path + "\\" + scenario_file_name
    scenario_output_file = open(output_file,"w+")
    json.dump(scenario, scenario_output_file)
    return output_path, output_file
