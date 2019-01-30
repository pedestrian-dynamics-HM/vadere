#!/usr/bin/env python3 

import os, sys
# This is just to make sure that the systems path is set up correctly, to have correct imports, it can be ignored:
sys.path.append(os.path.abspath("."))
sys.path.append(os.path.abspath(".."))

from tutorial.imports import *

run_local = True

###############################################################################################################
# Usecase: Set yourself the parameters you want to change. Do this by defining a list of dictionaries with the
# corresponding parameter. Again, the Vadere output is deleted after all scenarios run.

# Set own values to vary, they don't have to be the same - in the first run acceleration is left to default.
par_var = [{"speedDistributionMean": 1.0, "maximumSpeed": 3.0},
           {"speedDistributionMean": 1.3, "maximumSpeed": 4.0, "acceleration": 3.0}]

if __name__ == "__main__":  # main required by Windows to run in parallel

    setup = QuickVaryScenario(scenario_path=path2scenario,
                              parameter_var=par_var,
                              qoi="density.txt",
                              model=path2model)

    if run_local:
        par_var, data = setup.run(-1)  # -1 indicates to use all cores available to parallelize the scenarios
    else:
        par_var, data = setup.remote(-1)

    print("\n \n ---------------------------------------\n \n")
    print("ALL USED PARAMETER:")
    print(par_var)

    print("COLLECTED DATA:")
    print(data)
