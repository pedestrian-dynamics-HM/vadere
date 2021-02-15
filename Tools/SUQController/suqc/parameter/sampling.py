#!/usr/bin/env python3

from typing import *

import pandas as pd
import random

# http://scikit-learn.org/stable/modules/generated/sklearn.model_selection.ParameterSampler.html
from sklearn.model_selection import ParameterGrid
from suqc.environment import VadereEnvironmentManager
from suqc.utils.dict_utils import *

import abc
import copy

import numpy as np


class ParameterVariationBase(metaclass=abc.ABCMeta):

    MULTI_IDX_LEVEL0_PAR = "Parameter"
    MULTI_IDX_LEVEL0_LOC = "Location"
    ROW_IDX_NAME_ID = "id"

    def __init__(self):
        self._points = pd.DataFrame()

    @property
    def points(self):
        return self._points

    def nr_parameter_variations(self):
        nr_parameter_variations = len(self.points.index.levels[0])
        assert self.points.index.names[0] == "id"
        return nr_parameter_variations

    def nr_scenario_runs(self):
        # If this fails, then it is likely that the function self.multiply_scenario_runs was not called before
        nr_scenario_runs = len(self.points.index.levels[1])
        assert self.points.index.names[1] == "run_id"
        return nr_scenario_runs

    def multiply_scenario_runs(self, scenario_runs: Union[int, List[int]]):

        # scenario_runs can be a scalar value or a list
        # if it is a scalar value, each sample is repeated the same number of time
        # if it is a list, the int values co
        if isinstance(scenario_runs, list):
            if (
                all(
                    isinstance(scenario_run, int) and scenario_run > 0
                    for scenario_run in scenario_runs
                )
                == False
            ):
                raise ValueError(
                    f"Expect a list of positive integers. Got {scenario_runs}."
                )

            information = f"scenario_runs must contain {len(self._points.index.values)} elements. Got {len(scenario_runs)} elements."

            if len(scenario_runs) < len(self._points.index.values):
                raise ValueError(information)
            if len(scenario_runs) > len(self._points.index.values):
                print(
                    f"WARNING: {information}. Last {len(scenario_runs)-len(self._points.index.values)} element(s) are ignored."
                )

        if isinstance(scenario_runs, int):
            scenario_runs = scenario_runs * np.ones(
                (len(self._points.index.values),), dtype=int
            )

        k = 0
        for idx_vals in self._points.index.values:
            idx_id = idx_vals.repeat(scenario_runs[k])
            idx_run_id = np.arange(0, scenario_runs[k])
            df0 = np.tile(self._points.values[k], (scenario_runs[k], 1))
            if k == 0:
                idx_ids = idx_id
                idx_run_ids = idx_run_id
                df = df0
            else:
                idx_ids = np.append(idx_ids, idx_id)
                idx_run_ids = np.append(idx_run_ids, idx_run_id)

                df = np.append(df, df0, axis=0)
            k += 1

        self._points = pd.DataFrame(
            df,
            index=pd.MultiIndex.from_arrays(
                [idx_ids, idx_run_ids], names=["id", "run_id"]
            ),
            columns=self._points.columns,
        )

        self._points = self._points.sort_index(axis=1)

        return self

    def _add_dict_points(self, points: List[dict]):
        # NOTE: it may be required to generalize 'points' definition, at the moment it is assumed to be a list(grid),
        # where 'grid' is a ParameterGrid of scikit-learn

        # df = pd.concat([self._points, pd.DataFrame(points)], ignore_index=True, axis=0)
        # df.index.name = ParameterVariationBase.ROW_IDX_NAME_ID
        #
        # df.columns = pd.MultiIndex.from_product(
        #     [[ParameterVariationBase.MULTI_IDX_LEVEL0_PAR], df.columns]
        # )
        #

        dictionary = points[0]
        has_dict_sub_dicts_for_multiple_simulators = any(
            isinstance(i, dict) for i in dictionary.values()
        )

        if has_dict_sub_dicts_for_multiple_simulators:
            # if there are multiple simulators the dictionary is nested and looks like:
            # dictionary = { vadere : { para1 : val1, para2 : val 2} , omnet : { para1 : val1, para2 : val 2} }
            # The multiindex of the dataframe will contain the simulator name as additional level.
            # The following step produces two multiindex levels: parameter name, simulator name

            keys = dictionary.keys()
            df = pd.DataFrame()

            for key in keys:

                df_single = list()
                for point in points:
                    data = point[key]
                    df_single.append(data)

                df_single = pd.DataFrame(df_single)
                df_single.columns = pd.MultiIndex.from_product(
                    [[key], df_single.columns]
                )
                df = pd.concat([df, df_single], axis=1)

            cols = df.columns.values

        else:
            # if only vadere simulator is used, a simple dictionary like
            # dictionary = { para1 : val1, para2 : val 2}
            # is sufficient.
            # The multiindex of the dataframe does not contain the simulator name as additional level.
            # This refers to the original behavior of the suqc when only vadere was used as simulator.
            # The following step produces one multiindex levels: parameter name

            df = pd.concat(
                [self._points, pd.DataFrame(points)], ignore_index=True, axis=0
            )
            cols = [tuple([parameter]) for parameter in df.columns.values]

        # Add an additional multiindex levels called "Parameter"
        for ii in range(len(cols)):
            col = list(cols[ii])
            col.insert(0, ParameterVariationBase.MULTI_IDX_LEVEL0_PAR)
            cols[ii] = tuple(col)

        df.columns = pd.MultiIndex.from_tuples(cols)

        df.index.name = ParameterVariationBase.ROW_IDX_NAME_ID

        self._points = df

    def _add_df_points(self, points: pd.DataFrame):
        self._points = points

    def check_vadere_keys(self, scenario: dict, simulator="vadere"):

        keys = self._points.columns.get_level_values(-1)
        if self.is_multiple_simulators():
            keys = keys[self._points.columns.get_level_values(1) == simulator]

        for k in keys:
            try:  # check that the value is 'final' (i.e. not another sub-directory) and that the key is unique.
                deep_dict_lookup(
                    scenario, k, check_final_leaf=True, check_unique_key=True
                )
            except ValueError as e:
                raise e  # re-raise Exception
        return True

    def check_omnet_keys(self, inifile, simulator="omnet"):
        keys = self._points.columns.get_level_values(-1)
        if self.is_multiple_simulators():
            keys = keys[self._points.columns.get_level_values(1) == simulator]

        for k in keys:
            if k not in inifile.keys():
                raise ValueError("Key not found in omnet inifile.")
        return True

    def to_dictlist(self):
        return [i[1] for i in self.par_iter()]

    def par_iter(self, simulator=None):

        if self.is_multiple_simulators():  # vadere only
            df = self._points[(ParameterVariationBase.MULTI_IDX_LEVEL0_PAR, simulator)]
        else:
            df = self._points[ParameterVariationBase.MULTI_IDX_LEVEL0_PAR]

        for (par_id, run_id), row in df.iterrows():
            # TODO: this is not nice coding, however, there are some issues. See issue #40
            parameter_variation = dict(row)
            delete_keys = list()

            # nan entries are not considered and therefore removed
            for k, v in parameter_variation.items():
                if isinstance(v, np.float) and np.isnan(v):
                    delete_keys.append(k)

            for dk in delete_keys:
                del parameter_variation[dk]

            yield (par_id, run_id, parameter_variation)

    def is_multiple_simulators(self):
        return self._points.columns.nlevels == 3


class UserDefinedSampling(ParameterVariationBase):
    def __init__(self, points: List[dict]):
        super(UserDefinedSampling, self).__init__()
        self._add_dict_points(points)

    def add_vadere_server_id(self):

        ids = self.points.index.to_list()
        ids = [f'"vadere_Sample__{id[0]}_{id[1]}"' for id in ids]
        self.points.insert(0, ("Parameter", "omnet", "*.manager.host"), ids, True)
        self._points = self.points.sort_index(axis=1)

    def multiply_scenario_runs_using_seed(
        self, scenario_runs: Union[int, List[int]], seed_config: Dict
    ):
        if set(seed_config.keys()) != {"vadere", "omnet"}:
            raise ValueError(
                f"Dictionary keys must be: omnet, vadere. Got {set(seed_config.keys())}."
            )

        super().multiply_scenario_runs(scenario_runs)
        self.add_vadere_server_id()
        number_of_rows = self.points.shape[0]

        # omnet seed
        if seed_config["omnet"] == "fixed":
            pass  # use fixed seed defined in omnet ini file
        else:
            # use random seed for omnet
            seeds = [str(random.randint(1, 255)) for _ in range(number_of_rows)]
            self.points.insert(0, ("Parameter", "omnet", "seed-set"), seeds, True)

        # vadere seed
        if seed_config["vadere"] == "fixed":
            # use fixed seed defined in scenario file
            self.points.insert(
                0, ("Parameter", "omnet", "*.manager.useVadereSeed"), "true", True
            )
            self.points.insert(
                0,
                ("Parameter", "vadere", "attributesSimulation.useFixedSeed"),
                True,  # make sure that vadere uses a fixed seed
                True,
            )
        else:
            # use random seed in vadere provided from omnet ini file
            self.points.insert(
                0, ("Parameter", "omnet", "*.manager.useVadereSeed"), "false", True
            )
            seeds = [str(random.randint(1, 100000)) for _ in range(number_of_rows)]
            self.points.insert(0, ("Parameter", "omnet", "*.manager.seed"), seeds, True)

        self._points = self.points.sort_index(axis=1)

        return self


class FullGridSampling(ParameterVariationBase):
    def __init__(self, grid: Union[dict, ParameterGrid]):
        super(FullGridSampling, self).__init__()

        if isinstance(grid, dict):
            self._add_sklearn_grid(ParameterGrid(param_grid=grid))
        else:
            self._add_sklearn_grid(grid)

    def _add_sklearn_grid(self, grid: ParameterGrid):
        self._add_dict_points(
            points=list(grid)
        )  # list creates all points described by the 'grid'


class RandomSampling(ParameterVariationBase):

    # TODO: Check out ParameterSampler in scikit learn which I think combines random sampling with a grid.

    def __init__(self):
        super(RandomSampling, self).__init__()
        self._add_parameters = True
        self.dists = dict()

    def add_parameter(self, par: str, dist: np.random, **dist_pars: dict):
        assert self._add_parameters, (
            "The grid was already generated. For now it is not allowed to add more parameters "
            "afterwards"
        )
        self.dists[par] = {"dist": dist, "dist_pars": dist_pars}

    def _create_distribution_samples(self, nr_samples):

        samples = list()
        for i in range(nr_samples):
            samples.append({})

        for d in self.dists.keys():

            dist_args = deepcopy(self.dists[d]["dist_pars"])
            dist_args["size"] = nr_samples

            try:
                outcomes = self.dists[d]["dist"](**dist_args)
            except:
                raise RuntimeError(
                    f"Distribution {d} failed to sample. Every distribution has to support the keyword"
                    f" 'size'. It is recommended to use distributions from numpy: "
                    f"https://docs.scipy.org/doc/numpy-1.13.0/reference/routines.random.html"
                )

            for i in range(nr_samples):
                samples[i][d] = outcomes[i]

        return samples

    def create_grid(self, nr_samples=100):
        self._add_parameters = False
        samples = self._create_distribution_samples(nr_samples)
        self._add_dict_points(samples)


class BoxSamplingUlamMethod(ParameterVariationBase):
    def __init__(self):
        super(BoxSamplingUlamMethod, self).__init__()
        self._edges = None

    def _create_box_points(self, par, test_p):
        return [{par: test_p[i]} for i in range(len(test_p))]

    def _generate_interior_start(self, edges, nr_testf):

        boxes = len(edges) - 1
        arr = np.zeros(boxes * nr_testf)

        for i in range(boxes):
            s, e = edges[i : i + 2]
            arr[i * nr_testf : i * nr_testf + nr_testf] = np.linspace(
                s, e, nr_testf + 2
            )[1:-1]
        return arr

    def _get_box(self, row):

        vals = row.values

        def _get_idx(val, dim):
            return int(np.floor((val - self._edges[dim][0]) / self._box_width[dim]))

        idx_x = _get_idx(vals[0], 0)
        if len(vals) == 1:
            idx_y = 0
            idx_z = 0
        elif len(vals) == 2:
            idx_y = _get_idx(vals[1], 1)
            idx_z = 0
        else:
            idx_y = _get_idx(vals[1], 1)
            idx_z = _get_idx(vals[2], 2)

        box = (
            idx_x
            + idx_y * (self._nr_boxes[0])
            + idx_z * (self._nr_boxes[0] * self._nr_boxes[1])
        )
        return box

    def create_grid(self, par, lb, rb, nr_boxes, nr_testf):

        if isinstance(par, str):
            par = [par, None, None]

        if isinstance(lb, (float, int)):
            lb = [lb, 0, 0]

        if isinstance(rb, (float, int)):
            rb = [rb, 0, 0]

        if isinstance(nr_boxes, int):
            nr_boxes = [nr_boxes, 0, 0]

        if isinstance(nr_testf, int):
            nr_testf = [nr_testf, 0, 0]

        assert len(lb) == len(rb) == len(nr_boxes) == len(nr_testf) == 3

        self._nr_boxes = nr_boxes  # TODO: possible bring this in constructor

        self._edges = dict()
        self._box_width = dict()  # same initial setting

        for i in range(3):
            if par[i] is not None:
                # +1 bc. edges+1 = nr_boxes when the parameter
                self._edges[i] = np.linspace(lb[i], rb[i], nr_boxes[i] + 1)

                # the linspace guarantees equidistant box-domains
                self._box_width[i] = self._edges[i][1] - self._edges[i][0]

        # ^ y
        # |
        # |5 | 6 | 7 | 8 |
        # |1 | 2 | 3 | 4 |
        #  _________________>  x
        # o z (looking from above)
        #
        # If there is a 3rd parameter (z) the next slice starts on top of this (bottom-up)

        x_pos, y_pos, z_pos = [None, None, None]

        if par[0] is not None:
            x_pos = self._generate_interior_start(
                edges=self._edges[0], nr_testf=nr_testf[0]
            )

        if par[1] is not None:
            y_pos = self._generate_interior_start(
                edges=self._edges[1], nr_testf=nr_testf[1]
            )

        if par[2] is not None:
            z_pos = self._generate_interior_start(
                edges=self._edges[2], nr_testf=nr_testf[2]
            )

        mesh = np.meshgrid(x_pos, y_pos, z_pos, copy=True, indexing="xy")

        df_x, df_y, df_z = [None, None, None]

        df_x = pd.DataFrame(mesh[0].ravel(), columns=[par[0]])

        if par[1] is not None:
            df_y = pd.DataFrame(mesh[1].ravel(), columns=[par[1]])

        if par[2] is not None:
            df_z = pd.DataFrame(mesh[2].ravel(), columns=[par[2]])

        df_final = pd.concat([df_x, df_y, df_z], axis=1)
        df_final.columns = pd.MultiIndex.from_product(
            [[ParameterVariationBase.MULTI_IDX_LEVEL0_PAR], df_final.columns.values]
        )
        df_final.index.name = ParameterVariationBase.ROW_IDX_NAME_ID

        df_final["boxid"] = df_final.T.apply(self._get_box)

        self._add_df_points(points=df_final)

    def generate_markov_matrix(self, result):

        # bool_idx = np.isnan(result).any(axis=1)
        # result = result.loc[~bool_idx, :]

        def apply_result(point):
            row = point.iloc[:, 0]
            if np.isnan(row).any():
                return np.nan
            else:
                return self._get_box(row)

        idx = pd.IndexSlice
        box_start = self._points["boxid"]
        box_finish = result.loc[:, idx[:, "last"]].groupby(level=0).apply(apply_result)

        nr_boxes = box_start.max() + 1  # box ids start with 0

        markov = np.zeros([nr_boxes, nr_boxes])

        for i in range(nr_boxes):
            fboxes = box_finish.loc[box_start == i]
            vals, counts = np.unique(fboxes, return_counts=True)

            # make all nan boxes (usually happens when the ped is spawned into target) a self reference
            if np.isnan(vals).any():
                pos_nan = np.where(np.isnan(vals))

                # length bc. np.nan != np.nan --> therefore only count=1 entries in np.unique
                markov[i, i] = len(pos_nan[0])
                counts = np.delete(counts, pos_nan)
                vals = np.delete(vals, pos_nan)

            markov[i, vals.astype(np.int)] = counts

        bool_idx = markov.sum(axis=1).astype(np.bool)
        markov[bool_idx, :] = (
            markov[bool_idx, :] / markov[bool_idx, :].sum(axis=1)[:, np.newaxis]
        )

        return markov

    def compute_eig(self, markov):
        eigval, eigvec = np.linalg.eig(markov.T)
        idx = eigval.argsort()[::-1]
        eigval = eigval[idx]
        eigvec = eigvec[:, idx]
        return eigval, eigvec

    def uniform_distribution_over_boxes_included(self, points: pd.DataFrame):

        boxes_included = points.groupby(level=0, axis=0).apply(
            lambda row: self._get_box(row.iloc[0, :])
        )
        boxes_included = np.unique(boxes_included)

        all_boxes = self._points["boxid"].max() + 1

        initial_condition = np.zeros(all_boxes)
        initial_condition[boxes_included.astype(np.int)] = (
            1 / boxes_included.shape[0]
        )  # uniform

        return initial_condition

    def transfer_initial_condition(
        self, markov: np.array, initial_cond: np.array, nrsteps: int
    ):

        all_boxes = self._points["boxid"].max() + 1

        states = np.zeros([all_boxes, nrsteps + 1])
        states[:, 0] = initial_cond

        for i in range(1, nrsteps + 1):
            states[:, i] = markov.T @ states[:, i - 1]

        return states

    def _get_bar_data_from_state(self, state):
        # Note: only works for 2D as only this can be plotted

        all_boxes = self._points["boxid"].max() + 1

        x_dir = lambda box_id: (np.mod(box_id, self._nr_boxes[0])).astype(np.int)
        y_dir = lambda box_id: (box_id / self._nr_boxes[0]).astype(np.int)

        df = pd.DataFrame(
            0,
            index=np.arange(state.shape[0]),
            columns=["x", "y", "z", "dx", "dy", "dz"],
        )

        idx_edges_x = x_dir(np.arange(all_boxes))
        idx_edges_y = y_dir(np.arange(all_boxes))

        for i in range(idx_edges_x.shape[0]):
            df.loc[i, ["x", "y", "z"]] = [
                self._edges[0][idx_edges_x[i]],
                self._edges[1][idx_edges_y[i]],
                0,
            ]
            df.loc[i, ["dx", "dy", "dz"]] = [
                self._box_width[0],
                self._box_width[1],
                state[i],
            ]
        return df

    def plot_states(self, states, cols, rows):
        # https://matplotlib.org/gallery/mplot3d/3d_bars.html

        import matplotlib.pyplot as plt

        # This import registers the 3D projection, but is otherwise unused.

        fig = plt.figure(figsize=(8, 3))

        for sidx in range(states.shape[1]):

            ax = fig.add_subplot(cols, rows, sidx + 1, projection="3d")

            df = self._get_bar_data_from_state(states[:, sidx])

            zeros = df.loc[df["dz"] < 1e-3]
            nonzero = df.loc[df["dz"] != 0]

            ax.bar3d(
                zeros["x"],
                zeros["y"],
                zeros["z"],
                zeros["dx"],
                zeros["dy"],
                zeros["dz"],
                color="gray",
                shade=True,
            )
            ax.bar3d(
                nonzero["x"],
                nonzero["y"],
                nonzero["z"],
                nonzero["dx"],
                nonzero["dy"],
                nonzero["dz"],
                color="red",
                shade=True,
            )

            ax.set_xlabel("x")
            ax.set_ylabel("y")
            ax.set_zlabel("probability")
            ax.set_title(f"step={sidx}")
        plt.tight_layout()
        plt.show()

class RoverSampling(metaclass=abc.ABCMeta):
    def __init__(self, parameters=None, parameters_dependent=None):
        self.parameters = parameters
        self.parameters_dependent = parameters_dependent

    @abc.abstractmethod
    def get_sampling_vals(self):
        raise NotImplemented("Overwrite in child class.")

    def get_sampling(self):

        sample_vals = self.get_sampling_vals()

        par_var = list()

        for sample in sample_vals:
            pars = self.get_single_sample(sample)
            par_var.append(copy.deepcopy(pars))

        return par_var

    def __initialize_sample_dict(self):

        simulators = list()

        for parameter in self.parameters:
            sim = parameter.get_simulator()
            if sim is not None:
                simulators.append(sim)

        for parameter in self.parameters_dependent:
            sim = parameter.get_simulator()
            if sim is not None:
                simulators.append(sim)

        simulators = list(set(simulators))

        if len(simulators) > 0:
            pars = dict()
            for simulator in simulators:
                pars.update({simulator: {}})
        else:
            pars = {}
        return pars

    def get_single_sample(self, values):

        # if isinstance(values,float) or isinstance(values,int):
        #   values = [values]

        sample = self.__initialize_sample_dict()
        check = len(sample.keys())

        k = 0
        for parameter in self.parameters:

            if parameter.list_index is None:
                parameter.set_val(values[k])
                k += 1
            else:
                for index in parameter.list_index:
                    parameter.set_val(values[k], index)
                    k += 1

            if check == 0:
                sample.update(parameter.to_dict())
            else:
                simulator = parameter.get_simulator()
                sample[simulator].update(parameter.to_dict())

        for dep in self.parameters_dependent:

            dep.set_val(self.parameters)

            if check == 0:
                sample.update(dep.to_dict())
            else:
                simulator = dep.get_simulator()
                sample[simulator].update(dep.to_dict())

        return sample


class RoverSamplingLatinHyperCube(RoverSampling):
    def __init__(
        self, parameters=None, parameters_dependent=None, number_of_samples=10
    ):
        self.number_of_samples = number_of_samples
        super(RoverSamplingLatinHyperCube, self).__init__(
            parameters=parameters, parameters_dependent=parameters_dependent
        )

    def get_sampling_vals(self):

        from pyDOE import lhs

        number = 0
        for para in self.parameters:
            number = number + para.get_number_of_parameters()

        lhs_without_ranges = lhs(number, self.number_of_samples)
        lhs_mapped = lhs_without_ranges.copy()

        ind = 0
        for parameter in self.parameters:

            if parameter.get_number_of_parameters() == 1:

                interval = parameter.get_interval()
                lower_bound = parameter.get_lower_bound()

                lhs_mapped[:, ind] = lower_bound + lhs_mapped[:, ind] * interval
                ind += 1
            else:

                for c in range(parameter.get_number_of_parameters()):

                    interval = parameter.get_interval()[c]
                    lower_bound = parameter.get_lower_bound()[c]

                    lhs_mapped[:, ind] = lower_bound + lhs_mapped[:, ind] * interval
                    ind += 1

        return lhs_mapped


class RoverSamplingFullFactorial(RoverSampling):
    def __init__(self, parameters=None, parameters_dependent=None):
        super(RoverSamplingFullFactorial, self).__init__(
            parameters=parameters, parameters_dependent=parameters_dependent
        )

    def get_sampling_vals(self):

        par_var, x = list(), list()

        for para in self.parameters:
            x.append(para.get_stages())

        full_factorial = np.meshgrid(*x, indexing="ij")
        full_factorial = np.concatenate(np.transpose(full_factorial))

        if len(self.parameters) == 1:
            full_factorial = [[val] for val in full_factorial]

        return full_factorial


class Parameter:
    @classmethod
    def from_dict(cls, par_dict):
        pass

    def __init__(
        self,
        name,
        unit=None,
        simulator=None,
        value=None,
        range=None,
        list=None,
        list_index=None,
        stages=None,
    ):
        self.name = name
        self.value = value
        self.unit = unit
        self.simulator = simulator
        self.set_range(range)
        self.list_index = list_index
        self.list = list
        self.set_stages(stages)

    def get_stages(self):
        return self.stages

    def set_stages(self, stages):

        if isinstance(stages, int):
            range = self.get_range()
            stages = np.linspace(range[0], range[1], stages)

        self.stages = stages

    def get_val(self):
        return self.value

    def set_val(self, val, index=None):

        if (val - int(val)) == 0:
            val = int(val)

        if index is None:
            self.value = val
        else:
            if self.value is None:
                self.value = self.list
            self.value[index] = val

    def set_range(self, range):
        # reorder
        self.range = range

    def get_range(self):
        return self.range

    def get_interval(self):
        if self.list_index is None:
            interval = self.range[1] - self.range[0]
        else:
            interval = [item[1] - item[0] for item in self.range]
        return interval

    def get_lower_bound(self):
        if self.list_index is None:
            return self.range[0]
        else:
            return [item[0] for item in self.range]

    def get_upper_bound(self):
        if self.list_index is None:
            return self.range[1]
        else:
            return [item[1] for item in self.range]

    def get_simulator(self):
        return self.simulator

    def to_dict(self):

        if self.unit is None:
            val = self.value
        else:
            val = f"{self.value}{self.unit}"

        return {self.name: val}

    def get_number_of_parameters(self):
        if self.list_index is None:
            return 1
        else:
            return len(self.list_index)


class DependentParameter(Parameter):
    def __init__(
        self,
        name,
        equation=None,
        unit=None,
        simulator=None,
        value=None,
        range=None,
        list=None,
        list_index=None,
    ):

        self.equation = equation
        super().__init__(
            name=name,
            unit=unit,
            simulator=simulator,
            value=value,
            list=list,
            list_index=list_index,
        )

    def set_val(self, parameter=None):

        if callable(self.equation):
            # build argv for callable equation.
            if parameter is not None:
                argv = {p.name: p.get_val() for p in parameter}
            else:
                argv = {}
            function_val = self.equation(argv)
        else:
            # just set the value given in equation.
            function_val = self.equation

        if isinstance(function_val, float):
            if (function_val - int(function_val)) == 0:
                function_val = int(function_val)
        # to do for list
        self.value = function_val
