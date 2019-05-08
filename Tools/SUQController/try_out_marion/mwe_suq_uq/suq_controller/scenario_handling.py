#!/usr/bin/env python3
#  TODO: """ << INCLUDE DOCSTRING (one-line or multi-line) >> """
# TODO: Add necessary files for processors

# include imports after here:

import json
import os

# --------------------------------------------------
# Change default values
__authors__ = "Marion Gödel"
__credits__ = ["N/A"]  # insert names of people who reported bugs, made suggestions but didn't write code
#

PEDESTRIAN_POSITION_PROCESSOR = "org.vadere.simulator.projects.dataprocessing.processor.PedestrianPositionProcessor"
EVACUATION_TIME_PROCESSOR     = "org.vadere.simulator.projects.dataprocessing.processor.EvacuationTimeProcessor"
PEDESTRIAN_EVACUATION_TIME_PROCESSOR = "org.vadere.simulator.projects.dataprocessing.processor.PedestrianEvacuationTimeProcessor"
PEDESTRIAN_START_TIME_PROCESSOR = "org.vadere.simulator.projects.dataprocessing.processor.PedestrianStartTimeProcessor"



def set_spawn_number(scenario, value):
    scenario["scenario"]["topography"]["sources"][0]["spawnNumber"] = int(value)
    return scenario

def set_name(scenario, scenario_name):
    scenario["name"] = scenario_name
    return scenario

def get_timestep(scenario):
    return float(scenario["scenario"]["attributesSimulation"]["simTimeStepLength"])

def save_scenario(scenario, output_path, scenario_file_name):
    if not(os.path.isdir(output_path)):
        os.makedirs(output_path)
    json.dump(scenario, open(output_path + "/" + scenario_file_name, 'w'), indent=2)


def add_processor_for_qoi(scenario, qoi):
    if qoi ==  "evacuationTime":
        add_evac_time_processor(scenario, qoi)


def add_evac_time_processor(scenario, qoi):
    id_evac_time_processor = check_for_processor(scenario, PEDESTRIAN_EVACUATION_TIME_PROCESSOR)
    if id_evac_time_processor == -1:
        id_ped_evac_time_proc = check_for_processor(scenario, PEDESTRIAN_EVACUATION_TIME_PROCESSOR)
        if id_ped_evac_time_proc == -1:
            ped_evac_time_proc = add_pedestrian_evac_time_processor(scenario)
        else:
            ped_evac_time_proc = scenario["processWriters"]["processors"][id_ped_evac_time_proc-1]
        add_processor_with_dependency(scenario, EVACUATION_TIME_PROCESSOR, ped_evac_time_proc)


    #else:
    # evacuation time processor is present -> we assume all dependencies are fulfilled.



def check_add_processor(scenario, processor_type):
    id_processor = check_for_processor(scenario, processor_type)
    if id_processor == -1:  # not present
        processor = add_processor(scenario, processor_type)
    #else:
        # processor present. We assume all dependencies are fulfilled.
    return processor, id_processor


def add_pedestrian_evac_time_processor(scenario):
    id_ped_evac_time_processor = check_for_processor(scenario, PEDESTRIAN_EVACUATION_TIME_PROCESSOR)
    if id_ped_evac_time_processor == -1: # not present
        id_processor = number_of_processors(scenario)
        start_time_proc, __ = check_add_processor(scenario, PEDESTRIAN_START_TIME_PROCESSOR)
        ped_evac_time_proc = add_processor_with_dependency(scenario, PEDESTRIAN_EVACUATION_TIME_PROCESSOR, start_time_proc)
#     else: # pedestrian evac time processor present. We assume all dependencies are fulfilled.
    return ped_evac_time_proc



def number_of_processors(scenario):
    processor_list = scenario["processWriters"]["processors"]
    return len(processor_list)

def print_processor_list(scenario):
    processor_list = scenario["processWriters"]["processors"]
    print(processor_list)


def check_for_processor(scenario, processor):
    processor_list = scenario["processWriters"]["processors"]
    id_ped_pos = -1

    for i in range(0, len(processor_list) - 1):
        processor_i = processor_list[i]['type']
        if processor_i == processor:
            id_ped_pos = processor_list[i]['id']
            return id_ped_pos
    return -1



def add_processor(scenario, processor_type):
    id_processor = number_of_processors(scenario)
    new_processor = {'type': processor_type, 'id': id_processor+1} # first processor id is 1
    list.append(scenario["processWriters"]["processors"], new_processor)
    return new_processor

def get_processor_name_from_type(processor_type):
    parts_tmp = str.split(processor_type,".")
    processor_name = parts_tmp[len(parts_tmp)-1]
    return processor_name

def add_processor_with_dependency(scenario, processor_type, necessary_processor):
    processor_2_name = get_processor_name_from_type(necessary_processor["type"])
    id_name = processor_2_name[0].lower() + processor_2_name[1:len(processor_2_name)] + "Id"
    dep_proc_attributes = {id_name:necessary_processor["id"]}
    id_processor = number_of_processors(scenario)

    processor_1_name = get_processor_name_from_type(processor_type)
    attributes_name = "Attributes" + processor_1_name
    new_processor = {'type': processor_type, 'id': id_processor +1, 'attributesType': attributes_name, 'attributes': dep_proc_attributes} # first processor id is 1
    # add dependency

    list.append(scenario["processWriters"]["processors"], new_processor)
    return new_processor

def tmp(scenario):
    processor_list = scenario["processWriters"]["processors"]
    id_ped_pos = -1

    for i in range(0, len(processor_list)-1):
        processor = processor_list[i]['type']
        if processor ==  PEDESTRIAN_POSITION_PROCESSOR:
            id_ped_pos = processor_list[i]['id']
        if processor  == EVACUATION_TIME_PROCESSOR:
            break

    id_next_processor = number_of_processors(scenario)

    """ Evacuation time processor not yet present"""

    """ Dependencies for evacuation time processor: PedestrianEvacuationTimeProcessor, PedestrianStartTimeProcessor"""
    if id_ped_pos == -1:
        add_processor(scenario, PEDESTRIAN_POSITION_PROCESSOR, id_next_processor+1)
        id_next_processor+=1

    """ Add evacuation time processor"""

    id_evac_time_processor = id_next_processor+1
    evac_time_processor = {'type': EVACUATION_TIME_PROCESSOR, 'id': id_evac_time_processor}
    id_next_processor += 1
    # scenario["processWriters"]["processors"][id_next_processor] = evac_time_processor

    list.append(scenario["processWriters"]["processors"], evac_time_processor)

    """ Add file for evacuation time processor"""
    evac_time_processor_file = {'type': EVACUATION_TIME_PROCESSOR, 'id':id_evac_time_processor}

    list.append(scenario["processWriters"]["files"], evac_time_processor_file)
