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
par_var = [{"name": "g2_0_11", "sources.[id==-1].groupSizeDistribution": [0.0, 1.0], "minStepLength": 0.11},
           {"name": "g2_0_17", "sources.[id==-1].groupSizeDistribution": [0.0, 1.0], "minStepLength": 0.17},
           {"name": "g2_0_25", "sources.[id==-1].groupSizeDistribution": [0.0, 1.0], "minStepLength": 0.25},
           {"name": "g2_0_4625", "sources.[id==-1].groupSizeDistribution": [0.0, 1.0], "minStepLength": 0.4625},
           {"name": "g2_p18", "sources.[id==-1].groupSizeDistribution": [0.0, 1.0], "stepCircleResolution": 18, "minStepLength": 0.0,
                "minimumStepLength": False},
           {"name": "g2_p4", "sources.[id==-1].groupSizeDistribution": [0.0, 1.0], "stepCircleResolution": 4, "minStepLength": 0.0,
                "minimumStepLength": False},
           {"name": "g2_sievers16b", "sources.[id==-1].groupSizeDistribution": [0.0, 1.0],
            "stepLengthIntercept" : 0.235, "stepLengthSlopeSpeed" : 0.302, "minStepLength" : 0.235,
            "minimumStepLength" : True,
            },
           {"name": "g3_0_11", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 1.0], "minStepLength": 0.11},
           {"name": "g3_0_17", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 1.0], "minStepLength": 0.17},
           {"name": "g3_0_25", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 1.0], "minStepLength": 0.25},
           {"name": "g3_0_4625", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 1.0], "minStepLength": 0.4625},
           {"name": "g3_p18", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 1.0], "stepCircleResolution": 18, "minStepLength": 0.0,
            "minimumStepLength": False},
           {"name": "g3_p4", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 1.0], "stepCircleResolution": 4, "minStepLength": 0.0,
            "minimumStepLength": False},
           {"name": "g3_sievers16b", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 1.0],
            "stepLengthIntercept" : 0.235, "stepLengthSlopeSpeed" : 0.302, "minStepLength" : 0.235,
            "minimumStepLength" : True,
            },
           {"name": "g4_0_11", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 0.0, 1.0], "minStepLength": 0.11},
           {"name": "g4_0_17", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 0.0, 1.0], "minStepLength": 0.17},
           {"name": "g4_0_25", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 0.0, 1.0], "minStepLength": 0.25},
           {"name": "g4_0_4625", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 0.0, 1.0], "minStepLength": 0.4625},
           {"name": "g4_p18", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 0.0, 1.0], "stepCircleResolution": 18, "minStepLength": 0.0,
            "minimumStepLength": False},
           {"name": "g4_p4", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 0.0, 1.0], "stepCircleResolution": 4, "minStepLength": 0.0,
            "minimumStepLength": False},
           {"name": "g4_sievers16b", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 0.0, 1.0],
            "stepLengthIntercept" : 0.235, "stepLengthSlopeSpeed" : 0.302, "minStepLength" : 0.235,
            "minimumStepLength" : True,
            },
           {"name": "g5_0_11", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 0.0, 0.0, 1.0], "minStepLength": 0.11},
           {"name": "g5_0_17", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 0.0, 0.0, 1.0], "minStepLength": 0.17},
           {"name": "g5_0_25", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 0.0, 0.0, 1.0], "minStepLength": 0.25},
           {"name": "g5_0_4625", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 0.0, 0.0, 1.0], "minStepLength": 0.4625},
           {"name": "g5_p18", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 0.0, 0.0, 1.0], "stepCircleResolution": 18, "minStepLength": 0.0,
            "minimumStepLength": False},
           {"name": "g5_p4", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 0.0, 0.0, 1.0], "stepCircleResolution": 4, "minStepLength": 0.0,
            "minimumStepLength": False},
           {"name": "g5_sievers16b", "sources.[id==-1].groupSizeDistribution": [0.0, 0.0, 0.0, 0.0, 1.0],
            "stepLengthIntercept" : 0.235, "stepLengthSlopeSpeed" : 0.302, "minStepLength" : 0.235,
            "minimumStepLength" : True,
            }
           ]

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
