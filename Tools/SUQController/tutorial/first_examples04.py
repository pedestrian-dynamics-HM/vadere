#!/usr/bin/env python3 

import os, sys
# This is just to make sure that the systems path is set up correctly, to have correct imports, it can be ignored:
sys.path.append(os.path.abspath("."))
sys.path.append(os.path.abspath(".."))

from tutorial.imports import *

run_local = True

###############################################################################################################
# Usecase: FullVarySampling provides the most flexibility to provide a grid. All outputs are saved into the
# suqc_envs folder (either in the "[SRC_PATH]/suqc/suqc_envs" code or in "[HOME]/suqc_envs", depending on
# whether installed package or src is used directly.

# EnvironmentManager handles the "environment" (contains all vadere output and basis sceanrio). It is also suited
# to store the results.
env_man = EnvironmentManager.create_environment("test_remote", basis_scenario=path2scenario, replace=True)

# There are a few different sampling methods implemented. The abstract class ParameterVariation also makes it easy
# to implement new Sampling methods.
par_var = FullGridSampling(grid={"speedDistributionMean": np.array([1., 1.2])})

# Not documented here: postchanges, which allow to alter parameters of the scenario that should not be included in
# sampling. For example, changing the random seed, etc.

setup = FullVaryScenario(env_man=env_man, par_var=par_var, qoi="density.txt", model="vadere0_7rc.jar", njobs=1)

if run_local:
    par_lookup, data = setup.run(2)
else:
    par_lookup, data = setup.remote(2)

print(par_lookup)
print(data)
