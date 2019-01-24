#!/usr/bin/env python3 

# TODO: """ << INCLUDE DOCSTRING (one-line or multi-line) >> """

import abc

import numpy as np

from suqc.utils.dict_utils import change_dict

# --------------------------------------------------
# people who contributed code
__authors__ = "Daniel Lehmberg"
# people who made suggestions or reported bugs but didn't contribute code
__credits__ = ["n/a"]
# --------------------------------------------------

# TODO: is it possible to remove all "processor writers" from QoI that are not requested (via Quantity of Interest)
#  without too much ugly code? Furthermore, remove all processors for perfomance win. Do this **only** with explicit
#  user setting, otherwise the tool is blamed for not writing required data.
#  see issue #34


class ScenarioChanges(object):

    def __init__(self, apply_default=False):

        self._apply_scenario_changes = {}

        if apply_default:
            self._defaults()

    def _defaults(self):
        self.add_scenario_change(ChangeScenarioName())
        self.add_scenario_change(ChangeRealTimeSimTimeRatio())
        self.add_scenario_change(ChangeRandomNumber(par_id=True))
        self.add_scenario_change(AlwaysEnableMetaData())
        self.add_scenario_change(ChangeDescription())

    def add_scenario_change(self, sc: 'PostScenarioChange'):
        # ABCScenarioChange in '' to support forward reference,
        # see https://www.python.org/dev/peps/pep-0484/#forward-references
        if sc.name in self._apply_scenario_changes.keys():
            raise KeyError(f"Scenario change with {sc.name} is already present.")
        self._apply_scenario_changes[sc.name] = sc

    def _collect_changes(self, scenario, par_id, par_var):
        changes = {}
        for chn in self._apply_scenario_changes.values():
            changes.update(chn.get_changes_dict(scenario, par_id, par_var))
        return changes

    def change_scenario(self, scenario, par_id, par_var):
        return change_dict(scenario, changes=self._collect_changes(scenario, par_id, par_var))


class PostScenarioChange(metaclass=abc.ABCMeta):

    def __init__(self, name):
        self.name = name

    @abc.abstractmethod
    def get_changes_dict(self, scenario, par_id, par_var):
        raise NotImplementedError("ABC method")


class AlwaysEnableMetaData(PostScenarioChange):
    def __init__(self):
        super(AlwaysEnableMetaData, self).__init__(name="always_enable_meta_data")

    def get_changes_dict(self, scenario, par_id, par_var):
        return {"isWriteMetaData": True}


class ChangeRealTimeSimTimeRatio(PostScenarioChange):

    def __init__(self):
        super(ChangeRealTimeSimTimeRatio, self).__init__(name="real_time_sim_time_ratio")

    def get_changes_dict(self, scenario, par_id, par_var):
        return {"realTimeSimTimeRatio": 0.0}  # Speeds up the non-visual computations in case a ratio was set!


class ChangeRandomNumber(PostScenarioChange):
    KEY_FIXED = "useFixedSeed"
    KEY_SEED = "fixedSeed"
    KEY_SIM_SEED = "simulationSeed"

    def __init__(self, fixed=False, randint=False, par_id=False):
        assert fixed + randint + par_id == 1, "Exactly one parameter has to be set to true"
        self._isfixed = fixed
        self._fixed_randnr = None

        self._israndint = randint
        self._isparid = par_id

        super(ChangeRandomNumber, self).__init__(name="random_number")

    def set_fixed_random_nr(self, random_number: int):
        assert self._isfixed, "Modus has to be set to fixed"
        self._fixed_randnr = random_number

    def get_changes_dict(self, scenario, par_id, par_var):

        if self._isfixed:
            assert self._fixed_randnr is not None, "Fixed random number has to be set with method set_fixed_random_nr"
            rnr = self._fixed_randnr
        elif self._israndint:
            # 4294967295 = max unsigned 32 bit integer
            rnr = np.random.randint(0, 4294967295)
        else:  # --> self._isparid
            rnr = par_id

        return {ChangeRandomNumber.KEY_FIXED: True,
                ChangeRandomNumber.KEY_SEED: rnr,
                ChangeRandomNumber.KEY_SIM_SEED: rnr}


class ChangeScenarioName(PostScenarioChange):

    KEY_NAME = "name"

    def __init__(self):
        super(ChangeScenarioName, self).__init__(name="scenario_name")

    def get_changes_dict(self, scenario, par_id, par_var):
        existing = scenario[ChangeScenarioName.KEY_NAME]
        add_name = f"parid={par_id}"
        return {ChangeScenarioName.KEY_NAME: "_".join([existing, add_name])}


class ChangeDescription(PostScenarioChange):

    KEY_DESCRIPTION = "description"

    def __init__(self):
        super(ChangeDescription, self).__init__(name="description")
    
    def get_changes_dict(self, scenario, par_id, par_var):
        changes_in_description = " ".join(["applied parameter variation=", str(par_var)])
        return {ChangeDescription.KEY_DESCRIPTION: "--".join([f"par_id={par_id}", changes_in_description])}
