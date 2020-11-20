#!/usr/bin/env python3

import os
import sys

from tutorial.imports import *

# This is just to make sure that the systems path is set up correctly, to have correct imports, it can be ignored:
sys.path.append(os.path.abspath("."))
sys.path.append(os.path.abspath(".."))


run_local = True


if __name__ == "__main__":  # main required by Windows to run in parallel

    ###############################################################################################################
    # Usecase: FullVarySampling provides the most flexibility to provide a grid. All outputs are saved into the
    # suqc_envs folder (either in the "[SRC_PATH]/suqc/suqc_envs" code or in "[HOME]/suqc_envs", depending on
    # whether installed package or src is used directly.

    # EnvironmentManager handles the "environment" (contains all Vadere output and basis scenario). It is also suited
    # to store the results.
    env_man = VadereEnvironmentManager.create_variation_env(
        basis_scenario=path2scenario,
        base_path=None,
        env_name="test_remote",
        handle_existing="ask_user_replace",
    )

    # There are a few different sampling methods implemented. The abstract class ParameterVariation also makes it easy
    # to implement new Sampling methods.
    par_var = FullGridSampling(grid={"speedDistributionMean": np.array([1.0, 1.2])})
    par_var = par_var.multiply_scenario_runs(
        1
    )  # TODO: currently this is forced to run before it can handled!

    # Not documented here: postchanges, which allow to alter parameters of the scenario that should not be included in
    # sampling. For example, changing the random seed, etc.

    setup = VariationBase(
        env_man=env_man,
        parameter_variation=par_var,
        qoi="density.txt",
        model="vadere1_4.jar",
        njobs=1,
    )

    if run_local:
        par_lookup, data = setup.run(1)
    else:
        par_lookup, data = setup.remote(-1)

    print(par_lookup)
    print(data)
