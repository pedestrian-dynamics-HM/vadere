#!/usr/bin/env python3

import abc

import numpy as np

from suqc.utils.dict_utils import change_dict


class PostScenarioChangesBase(object):
    def __init__(self, apply_default=False):

        self._apply_scenario_changes = {}

        if apply_default:
            self._defaults()

    def _defaults(self):
        self.add_scenario_change(ChangeScenarioName())
        self.add_scenario_change(ChangeRealTimeSimTimeRatio())
        self.add_scenario_change(AlwaysEnableMetaData())
        self.add_scenario_change(ChangeDescription())

    def add_scenario_change(self, scenario_change: "PostScenarioChange"):
        # ABCScenarioChange in '' to support forward reference,
        # see https://www.python.org/dev/peps/pep-0484/#forward-references
        if scenario_change.name in self._apply_scenario_changes.keys():
            raise KeyError(
                f"Scenario change with {scenario_change.name} is already present."
            )
        self._apply_scenario_changes[scenario_change.name] = scenario_change

    def _collect_changes(self, scenario, parameter_id, run_id, parameter_variation):
        changes = {}
        for chn in self._apply_scenario_changes.values():
            changes.update(
                chn.get_changes_dict(
                    scenario=scenario,
                    parameter_id=parameter_id,
                    run_id=run_id,
                    parameter_variation=parameter_variation,
                )
            )
        return changes

    def change_scenario(self, scenario, parameter_id, run_id, parameter_variation):
        return change_dict(
            scenario,
            changes=self._collect_changes(
                scenario, parameter_id, run_id, parameter_variation
            ),
        )


class PostScenarioChange(metaclass=abc.ABCMeta):
    def __init__(self, name):
        self.name = name

    @abc.abstractmethod
    def get_changes_dict(self, scenario, parameter_id, run_id, parameter_variation):
        raise NotImplementedError("ABC method")


class AlwaysEnableMetaData(PostScenarioChange):
    def __init__(self):
        super(AlwaysEnableMetaData, self).__init__(name="always_enable_meta_data")

    def get_changes_dict(self, scenario, parameter_id, run_id, parameter_variation):
        return {"isWriteMetaData": True}


class ChangeRealTimeSimTimeRatio(PostScenarioChange):
    def __init__(self):
        super(ChangeRealTimeSimTimeRatio, self).__init__(
            name="real_time_sim_time_ratio"
        )

    def get_changes_dict(self, scenario, parameter_id, run_id, parameter_variation):
        return {
            "realTimeSimTimeRatio": 0.0
        }  # Speeds up the non-visual computations in case a ratio was set!


class ChangeRandomNumber(PostScenarioChange):
    KEY_FIXED = "useFixedSeed"
    KEY_SEED = "fixedSeed"
    KEY_SIM_SEED = "simulationSeed"

    def __init__(self, fixed=False, randint=False, par_and_run_id=False):
        assert (
            fixed + randint + par_and_run_id == 1
        ), "Exactly one parameter has to be set to true"
        self._isfixed = fixed
        self._fixed_randnr = None

        self._israndint = randint
        self._is_id_based = par_and_run_id

        super(ChangeRandomNumber, self).__init__(name="random_number")

    def set_fixed_random_nr(self, random_number: int):
        assert self._isfixed, "Modus has to be set to fixed"
        self._fixed_randnr = random_number

    def get_changes_dict(self, scenario, parameter_id, run_id, parameter_variation):

        if self._isfixed:
            assert (
                self._fixed_randnr is not None
            ), "Fixed random number has to be set with method set_fixed_random_nr"
            rnr = self._fixed_randnr
        elif self._israndint:
            # 4294967295 = max unsigned 32 bit integer
            rnr = np.random.randint(0, 4294967295)
        else:  # --> self._isparid
            rnr = parameter_id * 1e6 + run_id  # the 1E6 is required to not have

        return {
            ChangeRandomNumber.KEY_FIXED: True,
            ChangeRandomNumber.KEY_SEED: rnr,
            ChangeRandomNumber.KEY_SIM_SEED: rnr,
        }


class ChangeScenarioName(PostScenarioChange):

    KEY_NAME = "name"

    def __init__(self):
        super(ChangeScenarioName, self).__init__(name="scenario_name")

    def get_changes_dict(self, scenario, parameter_id, run_id, parameter_variation):
        existing = scenario[ChangeScenarioName.KEY_NAME]
        add_name = f"id{parameter_id}-run{run_id}"
        return {ChangeScenarioName.KEY_NAME: "_".join([existing, add_name])}


class ChangeDescription(PostScenarioChange):

    KEY_DESCRIPTION = "description"

    def __init__(self):
        super(ChangeDescription, self).__init__(name="description")

    def get_changes_dict(self, scenario, parameter_id, run_id, parameter_variation):
        changes_in_description = " ".join(
            ["applied parameter variation=", str(parameter_variation)]
        )
        return {
            ChangeDescription.KEY_DESCRIPTION: "--".join(
                [f"par_id={parameter_id} and run_id={run_id}", changes_in_description]
            )
        }
