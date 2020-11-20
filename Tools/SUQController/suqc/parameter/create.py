#!/usr/bin/env python3
import abc
import multiprocessing
import os
import warnings
from distutils.dir_util import copy_tree

import suqc.request  # no "from suqc.request import ..." works because of circular imports
from suqc.environment import AbstractEnvironmentManager, VadereEnvironmentManager
from suqc.parameter.postchanges import PostScenarioChangesBase
from suqc.parameter.sampling import ParameterVariationBase
from suqc.utils.dict_utils import change_dict, change_dict_ini, deep_dict_lookup
from suqc.utils.general import create_folder, njobs_check_and_set, remove_folder


class AbstractScenarioCreation(object):
    def __init__(
        self,
        env_man: AbstractEnvironmentManager,
        parameter_variation: ParameterVariationBase,
        post_change: PostScenarioChangesBase = None,
    ):
        self._env_man = env_man
        self._parameter_variation = parameter_variation
        self._post_changes = post_change
        self._sampling_check_selected_keys()

    @abc.abstractmethod
    def _sampling_check_selected_keys(self):
        raise NotImplemented

    @abc.abstractmethod
    def _sp_creation(self):
        raise NotImplemented

    @abc.abstractmethod
    def _mp_creation(self, njobs):
        raise NotImplemented

    # public methods
    def generate_scenarios(self, njobs):

        ntasks = self._parameter_variation.points.shape[0]
        njobs = njobs_check_and_set(njobs=njobs, ntasks=ntasks)

        # increases readability and promotes shorter paths (apparently lengthy paths can cause problems on Windows)
        # see issue #76
        self._adapt_nr_digits_env_man(
            nr_variations=self._parameter_variation.nr_parameter_variations(),
            nr_runs=self._parameter_variation.nr_scenario_runs(),
        )

        target_path = self._env_man.get_env_outputfolder_path()

        # For security:
        remove_folder(target_path)
        create_folder(target_path)

        if njobs == 1:
            request_item_list = self._sp_creation()
        else:
            request_item_list = self._mp_creation(njobs)

        return request_item_list

    # private methods
    def _adapt_nr_digits_env_man(self, nr_variations, nr_runs):
        self._env_man.nr_digits_variation = len(str(nr_variations))
        self._env_man.nr_digits_runs = len(str(nr_runs))

    ## vadere specific

    def _create_vadere_scenario(
        self, args
    ):  # TODO: how do multiple arguments work for pool.map functions? (see below)
        """Set up a new scenario and return info of parameter id and location."""
        parameter_id = args[0]  # TODO: this would kind of reduce this ugly code
        run_id = args[1]
        parameter_variation = args[2]

        par_var_scenario = change_dict(
            self._env_man.vadere_basis_scenario, changes=parameter_variation
        )

        if self._post_changes is not None:
            # Apply pre-defined changes to each scenario file
            new_scenario = self._post_changes.change_scenario(
                scenario=par_var_scenario,
                parameter_id=parameter_id,
                run_id=run_id,
                parameter_variation=parameter_variation,
            )
        else:
            new_scenario = par_var_scenario

        output_folder = self._env_man.get_variation_output_folder(parameter_id, run_id)
        self._print_scenario_warnings(new_scenario)
        scenario_path = self._env_man.save_scenario_variation(
            parameter_id, run_id, new_scenario
        )

        result_item = suqc.request.RequestItem(
            parameter_id=parameter_id,
            run_id=run_id,
            scenario_path=scenario_path,
            base_path=self._env_man.base_path,
            output_folder=output_folder,
        )
        return result_item

    def _print_scenario_warnings(self, scenario):
        try:
            real_time_sim_time_ratio, _ = deep_dict_lookup(
                scenario, "realTimeSimTimeRatio"
            )
        except Exception:
            real_time_sim_time_ratio = (
                0  # ignore this warning if the lookup failed for whatever reason.
            )

        if real_time_sim_time_ratio > 1e-14:
            warnings.warn(
                f"In a scenario the key 'realTimeSimTimeRatio={real_time_sim_time_ratio}'. Large values "
                f"slow down the evaluation speed."
            )

    ## omnet specific
    def _create_omnet_scenario(
        self, args
    ):  # TODO: how do multiple arguments work for pool.map functions? (see below)
        """Set up a new scenario and return info of parameter id and location."""
        parameter_id = args[0]  # TODO: this would kind of reduce this ugly code
        run_id = args[1]
        parameter_variation = args[2]

        par_var_scenario = change_dict_ini(
            self._env_man.omnet_basis_ini, changes=parameter_variation
        )
        output_path = self._env_man.scenario_variation_path(
            parameter_id, run_id, simulator="omnet"
        )

        with open(output_path, "w") as outfile:
            par_var_scenario.writer(outfile)

        folder = os.path.dirname(output_path)
        ini_path = os.path.join(self._env_man.env_path, "additional_rover_files")
        copy_tree(ini_path, folder)


class VadereScenarioCreation(AbstractScenarioCreation):
    def __init__(
        self,
        env_man: AbstractEnvironmentManager,
        parameter_variation: ParameterVariationBase,
        post_change: PostScenarioChangesBase = None,
    ):
        super().__init__(env_man, parameter_variation, post_change)

    def _sp_creation(self):
        """Single process loop to create all requested scenarios."""
        request_item_list = list()
        for par_id, run_id, par_change in self._parameter_variation.par_iter():
            request_item_list.append(
                self._create_vadere_scenario([par_id, run_id, par_change])
            )
        return request_item_list

    def _mp_creation(self, njobs):
        """Multi process function to create all requested scenarios."""
        pool = multiprocessing.Pool(processes=njobs)
        request_item_list = pool.map(
            self._create_vadere_scenario, self._parameter_variation.par_iter()
        )
        return request_item_list

    def _sampling_check_selected_keys(self):
        self._parameter_variation.check_vadere_keys(self._env_man.vadere_basis_scenario)


class CoupledScenarioCreation(AbstractScenarioCreation):
    def __init__(
        self,
        env_man: AbstractEnvironmentManager,
        parameter_variation: ParameterVariationBase,
        post_change: PostScenarioChangesBase = None,
    ):
        super().__init__(env_man, parameter_variation, post_change)

    def _sp_creation(self):
        """Single process loop to create all requested scenarios."""

        # omnet specific
        variations_omnet = self._parameter_variation.par_iter(simulator="omnet")
        for par_id, run_id, par_change in variations_omnet:
            self._create_omnet_scenario([par_id, run_id, par_change])

        # vadere specific
        request_item_list = list()
        variations_vadere = self._parameter_variation.par_iter(simulator="vadere")
        for par_id, run_id, par_change in variations_vadere:
            request_item_list.append(
                self._create_vadere_scenario([par_id, run_id, par_change])
            )

        return request_item_list

    def _mp_creation(self, njobs):
        """Multi process function to create all requested scenarios."""
        pool = multiprocessing.Pool(processes=njobs)

        variations_omnet = self._parameter_variation.par_iter(simulator="omnet")
        pool.map(self._create_omnet_scenario, variations_omnet)

        variations_vadere = self._parameter_variation.par_iter(simulator="vadere")
        request_item_list = pool.map(self._create_vadere_scenario, variations_vadere)
        return request_item_list

    def _sampling_check_selected_keys(self):

        self._parameter_variation.check_vadere_keys(self._env_man.vadere_basis_scenario)
        self._parameter_variation.check_omnet_keys(self._env_man.omnet_basis_ini)
