#!/usr/bin/env python3
#  TODO: """ << INCLUDE DOCSTRING (one-line or multi-line) >> """

# include imports after here:

import chaospy as cp
import json
import os
import pandas as pd
import subprocess
import matplotlib.pyplot as plt
import numpy as np

import suq_controller.scenario_handling as mapping
import suq_controller.query as dataquery
from os import path

# --------------------------------------------------
# Change default values
__authors__ = "Florian Künzner, Marion Gödel"
__credits__ = ["N/A"] # insert names of people who reported bugs, made suggestions but didn't write code
# --------------------------------------------------

""" This is a simple quickstart python program to show how to use chaospy
to quantify the uncertainty of a simple evacuation scenario that is
simulated with vadere. """



""" parameters """
scenario_file_name = "evac_master_marion.scenario"
source_id = 0 # id of source to be manipulated
output_scenario_path = "scenarios/evac_modified"
uncertain_parameter = "spawnNumber"
qoi = "evacuationTime"
input_scenario_path = path.abspath("../../vadere_io")



""" setup uncertain parameter """
number_of_peds_dist = cp.Normal(50, 1) # N(50,1)

""" generate nodes and weights """
nodes, weights = cp.generate_quadrature(3, number_of_peds_dist, rule="G")
nodes = nodes[0]

nodes_int = nodes.astype(int)

#print(nodes)
#print(weights)

""" propagate the uncertainty """
os.chdir(input_scenario_path)
evacuation_time = dataquery.run(scenario_file_name, uncertain_parameter, nodes, qoi, path.abspath(output_scenario_path))






""" Evaluation of runs """

#print(evacuation_time)
#print(nodes)

""" generate orthogonal polynomials for the distribution """
OP = cp.orth_ttr(3, number_of_peds_dist)

""" generate the general polynomial chaos expansion polynomial """
evacuation_times_GPCE = cp.fit_quadrature(OP, nodes, weights, evacuation_time)
evacuation_times_GPCE2 = cp.fit_quadrature(OP, nodes_int, weights, evacuation_time)



""" calculate statistics """
E_evacuation_time = cp.E(evacuation_times_GPCE, number_of_peds_dist)
E_evacuation_time2 = cp.E(evacuation_times_GPCE2, number_of_peds_dist)


StdDev_evacuation_time = cp.Std(evacuation_times_GPCE, number_of_peds_dist)

print(evacuation_times_GPCE)
print(number_of_peds_dist)

""" print the statistics """
print("mean evacuation time: %f" % E_evacuation_time)
print("mean evacuation time2: %f" % E_evacuation_time2)
print(E_evacuation_time - E_evacuation_time2)


print("stddev evacuation time: %f" % StdDev_evacuation_time)

plt.figure()
plt.plot(nodes, evacuation_time)

x = np.linspace(40,60,num=100)
y = evacuation_times_GPCE(x)

plt.plot(x, y)
plt.show()