#!/usr/bin/env python3

import sys

from tutorial.imports import *

# This is just to make sure that the systems path is set up correctly, to have correct imports.
# (It can be ignored)
sys.path.append(
    os.path.abspath(".")
)  # in case tutorial is called from the root directory
sys.path.append(os.path.abspath(".."))  # in tutorial directly

run_local = True

###############################################################################################################
# Usecase: One parameter in the scenario is changed, for every independent the data is collected and returned.
# The Vadere output is deleted after all scenarios run.

# Example where the values of 'speedDistributionMean' are set between 0.1 and 1.5 in 5 equidistant points

if __name__ == "__main__":  # mainly required by Windows to run in parallel

    setup = SingleKeyVariation(  # path to a Vadere .scenario file (the one to sample)
        scenario_path=path2scenario,
        # parameter key to change
        key="speedDistributionMean",
        # values to set for the parameter
        values=np.linspace(0.7, 1.5, 3),
        # output file name to collect
        qoi="density.txt",
        # path to Vadere console jar file or use
        # VadereConsoleWrapper for more options
        model=VadereConsoleWrapper(
            model_path=path2model, jvm_flags=["-enableassertions"]
        ),
        # specify how often each scenario should run
        scenario_runs=1,
        # post changes can be used to apply changes to the scenario that are not part of the
        # sampling -- especially random seed setting. It is easy to define user based changes.
        post_changes=PostScenarioChangesBase(apply_default=True),
        # specify the path, where the results are written
        output_path=os.path.abspath("."),
        # specify the folder to write vadere output files to
        output_folder="testfolder",
        # flag whether to remove the output_folder after the run
        remove_output=False,
    )

    if run_local:
        par_var, data = setup.run(njobs=-1)
    else:
        par_var, data = setup.remote(njobs=-1)

    print("---------------------------------------\n \n")
    print("ALL USED PARAMETER:")
    print(par_var)

    print("COLLECTED DATA:")
    print(data)
