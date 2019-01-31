#!/usr/bin/env python3 

import os, sys
# This is just to make sure that the systems path is set up correctly, to have correct imports, it can be ignored:
sys.path.append(os.path.abspath("."))
sys.path.append(os.path.abspath(".."))

from tutorial.imports import *


run_local = True

if __name__ == "__main__":  # main required by Windows to run in parallel
    ###############################################################################################################
    # Usecase: Provide a single Vadere scenario and location to write out the output to.


    setup = SingleScenarioOutput(path_scenario=path2scenario,
                                 path_output=os.path.join(path2tutorial, "example_output"),
                                 model=path2model,
                                 qoi=None)


    if run_local:
        setup.run() # provides only njobs=1 for single scenario
    else:
        setup.remote()


    ###############################################################################################################
    # Usecase: Provide a folder with more than .scenario file and an output folder. All scenarios are simulated, also
    # in parallel). Here it is not possible to hand in a quantity of interest, because there is no guarantee that
    # all scenarios have the same processors. Because there is only one example scenario file, only this will be
    # executed.


    setup = MultiScenarioOutput(path_scenarios=path2tutorial,
                                path_output=os.path.join(path2tutorial, "example_multirun_output"),
                                model=path2model)

    if run_local:
        res = setup.run(2)
    else:
        res = setup.remote(2)




