#!/usr/bin/env python3

import abc
from typing import *

import numpy as np
import pandas as pd
# http://scikit-learn.org/stable/modules/generated/sklearn.model_selection.ParameterSampler.html
from sklearn.model_selection import ParameterGrid

from suqc.environment import EnvironmentManager
from suqc.utils.dict_utils import *


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

    def multiply_scenario_runs(self, scenario_runs):

        idx_ids = self._points.index.values.repeat(scenario_runs)
        idx_run_ids = np.tile(np.arange(scenario_runs), self._points.shape[0])

        self._points = pd.DataFrame(self._points.values.repeat(scenario_runs, axis=0),
                                    index=pd.MultiIndex.from_arrays([idx_ids, idx_run_ids], names=["id", "run_id"]),
                                    columns=self._points.columns)
        return self

    def _add_dict_points(self, points: List[dict]):
        # NOTE: it may be required to generalize 'points' definition, at the moment it is assumed to be a list(grid),
        # where 'grid' is a ParameterGrid of scikit-learn

        df = pd.concat([self._points, pd.DataFrame(points)], ignore_index=True, axis=0)
        df.index.name = ParameterVariationBase.ROW_IDX_NAME_ID

        df.columns = pd.MultiIndex.from_product([[ParameterVariationBase.MULTI_IDX_LEVEL0_PAR], df.columns])
        self._points = df

    def _add_df_points(self, points: pd.DataFrame):
        self._points = points

    def check_selected_keys(self, scenario: dict):
        keys = self._points[ParameterVariationBase.MULTI_IDX_LEVEL0_PAR].columns

        for k in keys:
            try:  # check that the value is 'final' (i.e. not another sub-directory) and that the key is unique.
                deep_dict_lookup(scenario, k, check_final_leaf=True, check_unique_key=True)
            except ValueError as e:
                raise e  # re-raise Exception
        return True

    def to_dictlist(self):
        return [i[1] for i in self.par_iter()]

    def par_iter(self):

        for (par_id, run_id), row in self._points[ParameterVariationBase.MULTI_IDX_LEVEL0_PAR].iterrows():
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


class UserDefinedSampling(ParameterVariationBase):

    def __init__(self, points: List[dict]):
        super(UserDefinedSampling, self).__init__()
        self._add_dict_points(points)


class FullGridSampling(ParameterVariationBase):

    def __init__(self, grid: Union[dict, ParameterGrid]):
        super(FullGridSampling, self).__init__()
        
        if isinstance(grid, dict):
            self._add_sklearn_grid(ParameterGrid(param_grid=grid))
        else:
            self._add_sklearn_grid(grid)

    def _add_sklearn_grid(self, grid: ParameterGrid):
        self._add_dict_points(points=list(grid))         # list creates all points described by the 'grid'


class RandomSampling(ParameterVariationBase):

    # TODO: Check out ParameterSampler in scikit learn which I think combines random sampling with a grid.

    def __init__(self):
        super(RandomSampling, self).__init__()
        self._add_parameters = True
        self.dists = dict()

    def add_parameter(self, par: str, dist: np.random, **dist_pars: dict):
        assert self._add_parameters, "The grid was already generated. For now it is not allowed to add more parameters " \
                                     "afterwards"
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
                raise RuntimeError(f"Distribution {d} failed to sample. Every distribution has to support the keyword"
                                   f" 'size'. It is recommended to use distributions from numpy: "
                                   f"https://docs.scipy.org/doc/numpy-1.13.0/reference/routines.random.html")

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

        boxes = len(edges)-1
        arr = np.zeros(boxes * nr_testf)

        for i in range(boxes):
            s, e = edges[i:i+2]
            arr[i*nr_testf: i*nr_testf+nr_testf] = np.linspace(s, e, nr_testf+2)[1:-1]
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

        box = idx_x + idx_y * (self._nr_boxes[0]) + idx_z * (self._nr_boxes[0] * self._nr_boxes[1])
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
                self._edges[i] = np.linspace(lb[i], rb[i], nr_boxes[i]+1)

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
            x_pos = self._generate_interior_start(edges=self._edges[0], nr_testf=nr_testf[0])

        if par[1] is not None:
            y_pos = self._generate_interior_start(edges=self._edges[1], nr_testf=nr_testf[1])

        if par[2] is not None:
            z_pos = self._generate_interior_start(edges=self._edges[2], nr_testf=nr_testf[2])

        mesh = np.meshgrid(x_pos, y_pos, z_pos, copy=True, indexing="xy")

        df_x, df_y, df_z = [None, None, None]

        df_x = pd.DataFrame(mesh[0].ravel(), columns=[par[0]])

        if par[1] is not None:
            df_y = pd.DataFrame(mesh[1].ravel(), columns=[par[1]])

        if par[2] is not None:
            df_z = pd.DataFrame(mesh[2].ravel(), columns=[par[2]])

        df_final = pd.concat([df_x, df_y, df_z], axis=1)
        df_final.columns = pd.MultiIndex.from_product(
            [[ParameterVariationBase.MULTI_IDX_LEVEL0_PAR], df_final.columns.values])
        df_final.index.name = ParameterVariationBase.ROW_IDX_NAME_ID

        df_final["boxid"] = df_final.T.apply(self._get_box)

        self._add_df_points(points=df_final)

    def generate_markov_matrix(self, result):

        #bool_idx = np.isnan(result).any(axis=1)
        #result = result.loc[~bool_idx, :]

        def apply_result(point):
            row = point.iloc[:, 0]
            if np.isnan(row).any():
                return np.nan
            else:
                return self._get_box(row)

        idx = pd.IndexSlice
        box_start = self._points["boxid"]
        box_finish = result.loc[:, idx[:, "last"]].groupby(level=0).apply(apply_result)

        nr_boxes = box_start.max() + 1   # box ids start with 0

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
        markov[bool_idx, :] = markov[bool_idx, :] / markov[bool_idx, :].sum(axis=1)[:, np.newaxis]

        return markov

    def compute_eig(self, markov):
        eigval, eigvec = np.linalg.eig(markov.T)
        idx = eigval.argsort()[::-1]
        eigval = eigval[idx]
        eigvec = eigvec[:, idx]
        return eigval, eigvec

    def uniform_distribution_over_boxes_included(self, points: pd.DataFrame):

        boxes_included = points.groupby(level=0, axis=0).apply(lambda row: self._get_box(row.iloc[0, :]))
        boxes_included = np.unique(boxes_included)

        all_boxes = self._points["boxid"].max() + 1

        initial_condition = np.zeros(all_boxes)
        initial_condition[boxes_included.astype(np.int)] = 1 / boxes_included.shape[0]  # uniform

        return initial_condition

    def transfer_initial_condition(self, markov: np.array, initial_cond: np.array, nrsteps: int):

        all_boxes = self._points["boxid"].max() + 1

        states = np.zeros([all_boxes, nrsteps+1])
        states[:, 0] = initial_cond

        for i in range(1, nrsteps+1):
            states[:, i] = markov.T @ states[:, i-1]

        return states

    def _get_bar_data_from_state(self, state):
        # Note: only works for 2D as only this can be plotted

        all_boxes = self._points["boxid"].max() + 1

        x_dir = lambda box_id: (np.mod(box_id, self._nr_boxes[0])).astype(np.int)
        y_dir = lambda box_id: (box_id / self._nr_boxes[0]).astype(np.int)

        df = pd.DataFrame(0, index=np.arange(state.shape[0]), columns=["x", "y", "z", "dx", "dy", "dz"])

        idx_edges_x = x_dir(np.arange(all_boxes))
        idx_edges_y = y_dir(np.arange(all_boxes))

        for i in range(idx_edges_x.shape[0]):
            df.loc[i, ["x", "y", "z"]] = [self._edges[0][idx_edges_x[i]], self._edges[1][idx_edges_y[i]], 0]
            df.loc[i, ["dx", "dy", "dz"]] = [self._box_width[0], self._box_width[1], state[i]]
        return df

    def plot_states(self, states, cols, rows):
        # https://matplotlib.org/gallery/mplot3d/3d_bars.html

        import matplotlib.pyplot as plt
        # This import registers the 3D projection, but is otherwise unused.

        fig = plt.figure(figsize=(8, 3))

        for sidx in range(states.shape[1]):

            ax = fig.add_subplot(cols, rows, sidx+1, projection='3d')

            df = self._get_bar_data_from_state(states[:, sidx])

            zeros = df.loc[df["dz"] < 1E-3]
            nonzero = df.loc[df["dz"] != 0]

            ax.bar3d(zeros["x"], zeros["y"], zeros["z"], zeros["dx"], zeros["dy"], zeros["dz"], color="gray", shade=True)
            ax.bar3d(nonzero["x"], nonzero["y"], nonzero["z"], nonzero["dx"], nonzero["dy"], nonzero["dz"], color="red", shade=True)

            ax.set_xlabel("x")
            ax.set_ylabel("y")
            ax.set_zlabel("probability")
            ax.set_title(f"step={sidx}")
        plt.tight_layout()
        plt.show()


if __name__ == "__main__":
    par = BoxSamplingUlamMethod()
    par.create_grid(["dynamicElements.[id==1].position.x", "dynamicElements.[id==1].position.y", None],
                    lb=[0, 0, 0], rb=[20, 10, 0], nr_boxes=[20, 10, 0], nr_testf=[1, 1, 0])

    par.plot_states(initial_cond)

    exit()

    di = {"speedDistributionStandardDeviation": [0.0, 0.1, 0.2]}

    pd.options.display.max_columns = 4

    em = EnvironmentManager("corner")

    pv = BoxSamplingUlamMethod()
    pv.create_grid(["speedDistributionStandardDeviation", "speedDistributionMean", "minimumSpeed"], [0, 1, 2], [1, 2, 3], [2, 2, 2], [2, 2, 2])


    #pv = FullGridSampling()
    #pv.add_dict_grid({"speedDistributionStandardDeviation": [0.1, 0.2, 0.3, 0.4]})
    #print(pv.points)

    # pv = RandomSampling(em)
    # pv.add_parameter("speedDistributionStandardDeviation", np.random.normal)
    # pv.add_parameter("speedDistributionMean", np.random.normal)
    # pv.create_grid()
    #
    # print(pv._points)
