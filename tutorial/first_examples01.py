#!/usr/bin/env python3

import os
import sys

# This is just to make sure that the systems path is set up correctly, to have correct imports.
# (It can be ignored)
sys.path.append(os.path.abspath("."))   # in case tutorial is called from the root directory
sys.path.append(os.path.abspath(".."))  # in tutorial directly

from tutorial.imports import *

# --------------------------------------------------
# people who contributed code
__authors__ = "Daniel Lehmberg"
# people who made suggestions or reported bugs but didn't contribute code
__credits__ = ["n/a"]
# --------------------------------------------------

run_local = True

###############################################################################################################
# Usecase: One parameter in the scenario is changed, for every independent the data is collected and returned.
# The Vadere output is deleted after all scenarios run.

# Example where the values of 'speedDistributionMean' are set between 0.1 and 1.5 in 5 equidistant points


setup = SingleKeyVaryScenario(scenario_path=path2scenario,  # -> path to the Vadere .scenario file to vary
                              key="speedDistributionMean",  # -> parameter key to change
                              values=np.linspace(0.7, 1.5, 3),  # -> values to set for the parameter
                              qoi="density.txt",  # -> output file name to collect
                              model=path2model)  # -> path to Vadere console jar file to use for simulation


if run_local:
    par_var, data = setup.run(njobs=1)
else:
    par_var, data = setup.remote(njobs=3)


print("---------------------------------------\n \n")
print("ALL USED PARAMETER:")
print(par_var)

print("COLLECTED DATA:")
print(data)