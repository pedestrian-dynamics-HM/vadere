from typing import Union, List

import pandas as pd
import os
import shutil
import numpy
import glob

from imports import path2tutorial
from suqc import (
    CoupledEnvironmentManager,
    CoupledScenarioCreation,
    ParameterVariationBase,
)
from utils.general import user_query_yes_no


def retrieve_rover_sim(
    samples_to_repeat: Union[tuple, List[tuple]],
    meta_info_file: str,
    env_info_file: str,
    env=None,
):

    if isinstance(samples_to_repeat, list):
        if (
            all(
                isinstance(sampl_to_repeat, tuple)
                for sampl_to_repeat in samples_to_repeat
            )
            is False
        ):
            raise ValueError(
                f"Expect a list of tuples like (id,run_id). Got {samples_to_repeat}."
            )

    if isinstance(samples_to_repeat, tuple):
        samples_to_repeat = [samples_to_repeat]

    if os.path.splitext(meta_info_file)[1] != ".pkl":
        raise ValueError(f"meta_info_file must be a *.pkl file. Got {meta_info_file}.")

    if os.path.splitext(env_info_file)[1] != ".pkl":
        raise ValueError(f"env_info_file must be a *.pkl file. Got {env_info_file}.")

    if os.path.exists(env):
        answer = user_query_yes_no(
            f"{env} already exists. Delete current environment and proceed?"
        )
        if answer is True:
            shutil.rmtree(env)
            os.makedirs(env)
        else:
            exit()

    df = pd.read_pickle(meta_info_file)
    df.columns = pd.MultiIndex.from_tuples(df.columns.tolist())
    df = df.iloc[:, df.columns.get_level_values(0) == "Parameter"]
    df = df.loc[samples_to_repeat, :]

    coupled_env = CoupledEnvironmentManager.create_variation_env_from_info_file(
        os.path.join(env, env_info_file)
    )

    par = ParameterVariationBase()
    par._add_df_points(df)

    cou = CoupledScenarioCreation(env_man=coupled_env, parameter_variation=par,)
    cou.generate_scenarios(1)

    print(f"INFO: Copied sample(s) {samples_to_repeat} to {env}.")
    print("INFO: Please start the simulations manually.")


if __name__ == "__main__":

    # retrieve data from pandas/dataframes pickle exports
    dir_with_info_df = os.path.join(path2tutorial, "first_examples_rover_01_df")
    meta_info_file = os.path.join(dir_with_info_df, "metainfo.pkl")
    env_info_file = os.path.join(dir_with_info_df, "envinfo.pkl")

    # put the retrieved simulations in following directory
    env = os.path.join(path2tutorial, "first_examples_rover_01")

    retrieve_rover_sim(
        samples_to_repeat=[(0, 0), (1, 0)],
        meta_info_file=meta_info_file,
        env_info_file=env_info_file,
        env=env,
    )
