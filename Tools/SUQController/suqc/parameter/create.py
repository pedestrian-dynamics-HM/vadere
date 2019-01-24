#!/usr/bin/env python3 

# TODO: """ << INCLUDE DOCSTRING (one-line or multi-line) >> """

import multiprocessing

from suqc.environment import EnvironmentManager
from suqc.parameter.sampling import ParameterVariation
from suqc.parameter.postchanges import ScenarioChanges

from suqc.utils.general import create_folder, remove_folder, njobs_check_and_set
from suqc.utils.dict_utils import change_dict

# --------------------------------------------------
# people who contributed code
__authors__ = "Daniel Lehmberg"
# people who made suggestions or reported bugs but didn't contribute code
__credits__ = ["n/a"]
# --------------------------------------------------


class VadereScenarioCreation(object):

    def __init__(self, env_man: EnvironmentManager, par_var: ParameterVariation, sc_change: ScenarioChanges=None):
        self._env_man = env_man
        self._par_var = par_var
        self._sc_change = sc_change

        self._basis_scenario = self._env_man.basis_scenario
        self._par_var.check_selected_keys(self._basis_scenario)

    def _vars_dict(self, pid, fp, op):
        return {"par_id": pid, "scenario_path": fp, "output_path": op}

    def _create_new_vadere_scenario(self, scenario: dict, par_id: int, par_var: dict):

        par_var_scenario = change_dict(scenario, changes=par_var)

        if self._sc_change is not None:
            # Apply pre-defined changes to each scenario file
            final_scenario = self._sc_change.change_scenario(scenario=par_var_scenario, par_id=par_id, par_var=par_var)
        else:
            final_scenario = par_var_scenario

        return final_scenario

    def _save_vadere_scenario(self, par_id, s):
        fp = self._env_man.save_scenario_variation(par_id, s)
        return fp

    def _create_scenario(self, args):  # TODO: how do multiple arguments work for pool.map functions? (see below)
        """Set up a new scenario and return info of parameter id and location."""
        par_id = args[0]  # TODO: this would kind of reduce this ugly code
        par_change = args[1]

        new_scenario = self._create_new_vadere_scenario(self._basis_scenario, par_id, par_change)

        output_path = self._env_man.get_par_id_output_path(par_id, create=False)
        scenario_path = self._save_vadere_scenario(par_id, new_scenario)

        return self._vars_dict(par_id, scenario_path, output_path)

    def _sp_creation(self):
        """Single process loop to create all requested scenarios."""
        basis_scenario = self._env_man.basis_scenario
        self._par_var.check_selected_keys(basis_scenario)

        vars_ = list()
        for par_id, par_change in self._par_var.par_iter():
            vars_.append(self._create_scenario([par_id, par_change]))
        return vars_

    def _mp_creation(self, njobs):
        """Multi process function to create all requested scenarios."""
        pool = multiprocessing.Pool(processes=njobs)
        vars_ = pool.map(self._create_scenario, self._par_var.par_iter())
        return vars_

    def generate_vadere_scenarios(self, njobs):

        njobs = njobs_check_and_set(njobs=njobs, ntasks=self._par_var.points.shape[0])

        target_path = self._env_man.get_env_outputfolder_path()

        remove_folder(target_path)
        create_folder(target_path)

        if njobs == 1:
            vars_ = self._sp_creation()
        else:
            vars_ = self._mp_creation(njobs)

        return vars_
