#!/usr/bin/env python3

import os
import sys

from tutorial.imports import *

# This is just to make sure that the systems path is set up correctly, to have correct imports, it can be ignored:
sys.path.append(os.path.abspath("."))
sys.path.append(os.path.abspath(".."))


run_local = True

# NOTE: If running this script twice, there is an user input required. Because an "output folder" already exists from
# the first run, this output folder gets replaced with the next run. Therefore, the old output is removed.

if __name__ == "__main__":  # main required by Windows to run in parallel
    ###############################################################################################################
    # Usecase: Provide a single Vadere scenario and location to write out the output to.

    setup = SingleExistScenario(
        path_scenario=path2scenario,
        qoi=None,
        model=path2model,
        scenario_runs=2,
        output_path=path2tutorial,
        output_folder=None,
    )

    if run_local:
        res = setup.run()  # provides only njobs=1 for single scenario
    else:
        res = setup.remote()

    print(res)

    ###############################################################################################################
    # Usecase: Provide a folder with more than .scenario file and an output folder. All scenarios are simulated, also
    # in parallel). Here it is not possible to hand in a quantity of interest, because there is no guarantee that
    # all scenarios have the same processors. Because there is only one example scenario file, only this will be
    # executed.

    setup = FolderExistScenarios(
        path_scenario_folder=path2tutorial,
        model=path2model,
        scenario_runs=2,
        output_path=path2tutorial,
        output_folder="example_multirun_output",
    )

    if run_local:
        res = setup.run(1)
    else:
        res = setup.remote(1)

    print(res)
