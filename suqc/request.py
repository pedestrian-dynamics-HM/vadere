#!/usr/bin/env python3

# TODO: """ << INCLUDE DOCSTRING (one-line or multi-line) >> """

import os
import shutil
import multiprocessing

from suqc.qoi import QuantityOfInterest
from suqc.environment import VadereConsoleWrapper
from suqc.parameter.sampling import *
from suqc.parameter.postchanges import ScenarioChanges
from suqc.parameter.create import VadereScenarioCreation
from suqc.utils.general import create_folder, check_parent_exists_folder_remove, njobs_check_and_set, str_timestamp
from suqc.remote import ServerRequest, ServerConnection
from suqc.configuration import SuqcConfig

from typing import *

# --------------------------------------------------
# people who contributed code
__authors__ = "Daniel Lehmberg"
# people who made suggestions or reported bugs but didn't contribute code
__credits__ = ["n/a"]
# --------------------------------------------------


class Request(object):

    def __init__(self, query_list: List[dict], model: Union[str, VadereConsoleWrapper],
                 qoi: Union[QuantityOfInterest, None]):

        model = VadereConsoleWrapper.infer_model(model)

        self.qoi = qoi  # Can be None, if this is the case, no data will be written
        self.model = model

        self.container = query_list
        self._add_return_keys()

        # Return values
        self.qoi_data = None
        self.req_time = None

    def _add_return_keys(self):
        for d in self.container:
            d["data"] = None
            d["req_time"] = np.nan

    def _interpret_return_value(self, ret_val, par_id):
        if ret_val == 0:
            return True
        else: # ret_val == 1:
            print(f"WARNING: Simulation with parameter setting {par_id} failed.")
            return False

    def _single_request(self, container_element) -> Tuple[dict, np.float64]:
        par_id = container_element["par_id"]
        scenario_path = container_element["scenario_path"]
        output_path = container_element["output_path"]

        self._create_output_path(output_path)

        ret_val, req_time = self.model.run_simulation(scenario_path, output_path)
        is_results = self._interpret_return_value(ret_val, par_id)

        if is_results and self.qoi is not None:
            result = self.qoi.read_and_extract_qois(par_id, output_path)
        else:
            result = None

        if self.qoi is not None and not is_results:
            req_time = np.nan

        container_element["data"] = result
        container_element["req_time"] = req_time

        # because of the multi-processor, don't try to already add the results here to _results_df
        return container_element

    def _create_output_path(self, output_path):
        create_folder(output_path, delete_if_exists=True)
        return output_path

    def _finalize_qoi(self):

        qoi_results = [r["data"] for r in self.container]

        filenames = None
        for ires in qoi_results:
            if ires is not None:
                # it is assumed that the the keys for all elements in results are the same!
                filenames = list(ires.keys())
                break

        if filenames is None:
            print("WARNING: All simulations failed, only 'None' results.")
            final_results = None
        else:
            # Successful runs are collected and are concatenated into a single pd.DataFrame below
            final_results = dict()

            for f in filenames:

                collected_data = list()

                for ires in qoi_results:
                    if ires is not None:
                        collected_data.append(ires[f])

                collected_data = pd.concat(collected_data, axis=0)
                final_results[f] = collected_data

        if filenames is not None and len(filenames) == 1:
            # there is no need to have the key/value if only file was requested
            final_results = final_results[filenames[0]]

        return final_results

    def _finalize_req_time(self):
        idx = [r["par_id"] for r in self.container]
        val = [r["req_time"] for r in self.container]
        return pd.Series(val, idx)

    def _sp_query(self):
        # enumerate returns tuple(par_id, scenario filepath) see ParameterVariation.generate_vadere_scenarios and
        # ParameterVariation._vars_object()
        results = list()
        for arg in self.container:
            res = self._single_request(arg)
            results.append(res)

    def _mp_query(self, njobs):
        pool = multiprocessing.Pool(processes=njobs)
        all_results = pool.map(self._single_request, self.container)

        # Data has to be copied from the results into the local container
        for k, res in enumerate(all_results):
            el = self.container[k]
            assert el["par_id"] == res["par_id"]
            self.container[k]["data"] = res["data"]
            self.container[k]["req_time"] = res["req_time"]

    def run(self, njobs: int = 1):

        assert not isinstance(njobs, int) or njobs != 0 or njobs < -1, \
            "njobs has to be an integer and cannot be zero or smaller than -1"

        nr_simulations = len(self.container)  # nr of rows = nr of parameter settings = #simulations
        njobs = njobs_check_and_set(njobs=njobs, ntasks=nr_simulations)

        if njobs == 1:
            self._sp_query()
        else:
            self._mp_query(njobs=njobs)

        if self.qoi is not None:
            self.qoi_data = self._finalize_qoi()

        self.req_time = self._finalize_req_time()

        return self.qoi_data, self.req_time


class FullVaryScenario(Request, ServerRequest):

    def __init__(self, env_man: EnvironmentManager, par_var: ParameterVariation, model: str,
                 qoi: Union[str, List[str], QuantityOfInterest], sc_change: ScenarioChanges = None, njobs: int = 1):

        self.njobs = njobs
        self.par_var = par_var
        self.env_man = env_man
        self.sc_change = sc_change
        self.model = model

        if isinstance(qoi, (str, list)):
            self.qoi = QuantityOfInterest(basis_scenario=self.env_man.basis_scenario, requested_files=qoi)
        else:
            self.qoi = qoi

        vadcreate = VadereScenarioCreation(self.env_man, self.par_var, self.sc_change)
        query_list = vadcreate.generate_vadere_scenarios(njobs)

        super(FullVaryScenario, self).__init__(query_list, self.model, self.qoi)

    def run(self, njobs: int = 1):
        qoi_result, req_time = super(FullVaryScenario, self).run(njobs)

        par_lookup = self.par_var.points.copy(deep=True)
        par_lookup["req_time"] = req_time

        return par_lookup, qoi_result

    @classmethod
    def _remote_run(cls, remote_pickle_arg_path, remote_pickle_res_path, remote_env_name, njobs):
        kwargs = cls.open_arg_pickle(remote_pickle_arg_path)

        setup = cls(env_man=EnvironmentManager(remote_env_name),
                    par_var=kwargs["par_var"],
                    qoi=kwargs["qoi"],
                    model=kwargs["model"])

        res = setup.run(njobs)
        cls.dump_result_pickle(res, remote_pickle_res_path)

    def remote(self, njobs):

        with ServerConnection() as sc:

            self.setup_connection(sc)

            self._transfer_local2remote(self.env_man.path_basis_scenario)
            remote_model_path = self._transfer_model_local2remote(self.model)

            pickle_content = {"par_var": self.par_var, "qoi": self.qoi, "model": remote_model_path,
                              "sc_change": self.sc_change}

            remote_pickle_arg_path = self._transfer_pickle_local2remote(**pickle_content)
            remote_pickle_res_path = self._join_linux_path([self.remote_folder_path, "result.p"], is_folder=False)

            s = f"""python3 -c 'import suqc; suqc.FullVaryScenario._remote_run("{remote_pickle_arg_path}", "{remote_pickle_res_path}", "{self.remote_env_name}", {njobs})'"""

            self.server.con.run(s)

            local_pickle_path = os.path.join(self.env_man.get_env_outputfolder_path(), "result.p")
            res = self._transfer_pickle_remote2local(remote_pickle_res_path, local_pickle_path)

            self._remove_remote_folder()

        return res


class QuickVaryScenario(FullVaryScenario, ServerRequest):

    def __init__(self, scenario_path: str, parameter_var: List[dict], qoi: Union[str, List[str]],
                 model: Union[str, VadereConsoleWrapper], env_remote=None):

        self.scenario_path = scenario_path

        assert os.path.exists(scenario_path) and scenario_path.endswith(".scenario"), \
            "Filepath must exist and the file has to end with .scenario"

        # TODO: find a way to "finally" remore this folder (even when exception occured!)
        # results are only returned, not saved, but output has to be saved, the removed again.
        self.temporary_env_name = "_".join(["temporary", os.path.basename(scenario_path).replace(".scenario", ""),
                                            str_timestamp()])

        if env_remote is None:
            env = EnvironmentManager.create_environment(
                env_name=self.temporary_env_name, basis_scenario=self.scenario_path, replace=True)
            self.tmp_folder_path = env.env_path

        else:
            self.temporary_env_name = env_remote.env_path
            self.tmp_folder_path = None  # Do not remove the folder because this is done with the remote procedure
            env = env_remote

        par_var = UserDefinedSampling(parameter_var)

        # there is no need to have more than 1 processor, for user defined input
        super(QuickVaryScenario, self).__init__(env, par_var, model, qoi, None, 1)

    def _remove_temporary_path(self):
        if self.tmp_folder_path is not None:
            shutil.rmtree(self.tmp_folder_path)

    def run(self, njobs: int = 1):
        res = super(QuickVaryScenario, self).run(njobs)
        self._remove_temporary_path()
        return res

    @classmethod
    def _remote_run(cls, remote_pickle_arg_path, remote_pickle_res_path, remote_env_name, njobs):
        kwargs = cls.open_arg_pickle(remote_pickle_arg_path)

        setup = QuickVaryScenario(scenario_path=kwargs["sp"],
                                  parameter_var=kwargs["par_var"],
                                  qoi=kwargs["qoi"],
                                  model=kwargs["model"],
                                  env_remote=EnvironmentManager(remote_env_name))

        res = setup.run(njobs)
        cls.dump_result_pickle(res, remote_pickle_res_path)

    def remote(self, njobs):

        with ServerConnection() as sc:
            self.setup_connection(sc)

            remote_scenario_path = self._transfer_local2remote(self.scenario_path)
            remote_model_path = self._transfer_model_local2remote(self.model)

            pickle_content = {"sp": remote_scenario_path, "par_var": self.par_var.to_dictlist(), "qoi": self.qoi, "model": remote_model_path}

            # TODO: duplicated code!
            remote_pickle_arg_path = self._transfer_pickle_local2remote(**pickle_content)
            remote_pickle_res_path = self._join_linux_path([self.remote_folder_path, "result.p"], is_folder=False)

            s = f"""python3 -c 'import suqc; suqc.QuickVaryScenario._remote_run("{remote_pickle_arg_path}", "{remote_pickle_res_path}", "{self.remote_env_name}", {njobs})'"""

            self.server.con.run(s)

            local_pickle_path = os.path.join(SuqcConfig.path_container_folder(), "result.p")
            res = self._transfer_pickle_remote2local(remote_pickle_res_path, local_pickle_path)

            self._remove_remote_folder()

        self._remove_temporary_path()

        return res


class SingleKeyVaryScenario(QuickVaryScenario, ServerRequest):

    def __init__(self, scenario_path: str, key: str, values: np.ndarray, qoi: Union[str, List[str]], model: str, env_remote=None):
        self.key = key
        self.values = values

        simple_grid = [{key: v} for v in values]
        super(SingleKeyVaryScenario, self).__init__(scenario_path, simple_grid, qoi, model, env_remote)

    @classmethod
    def _remote_run(cls, remote_pickle_arg_path, remote_pickle_res_path, remote_env_name, njobs):
        kwargs = cls.open_arg_pickle(remote_pickle_arg_path)

        setup = cls(scenario_path=kwargs["sp"],
                    key=kwargs["key"],
                    values=kwargs["val"],
                    qoi=kwargs["qoi"],
                    model=kwargs["model"],
                    env_remote=EnvironmentManager(remote_env_name))

        res = setup.run(njobs)
        cls.dump_result_pickle(res, remote_pickle_res_path)

    def remote(self, njobs):

        with ServerConnection() as sc:

            self.setup_connection(sc)

            remote_scenario_path = self._transfer_local2remote(self.scenario_path)
            remote_model_path = self._transfer_model_local2remote(self.model)

            pickle_content = {"sp": remote_scenario_path, "key": self.key, "val": self.values, "qoi": self.qoi,
                              "model": remote_model_path}

            remote_pickle_arg_path = self._transfer_pickle_local2remote(**pickle_content)
            remote_pickle_res_path = self._join_linux_path([self.remote_folder_path, "result.p"], is_folder=False)

            s = f"""python3 -c 'import suqc; suqc.SingleKeyVaryScenario._remote_run("{remote_pickle_arg_path}", "{remote_pickle_res_path}", "{self.remote_env_name}", {njobs})'"""
            self.server.con.run(s)

            local_pickle_path = os.path.join(self.tmp_folder_path, "result.p")
            res = self._transfer_pickle_remote2local(remote_pickle_res_path, local_pickle_path)

        self._remove_temporary_path()
        return res


class MultiScenarioOutput(Request, ServerRequest):

    def __init__(self, path_scenarios, path_output, model):
        query_list = list()
        self.path_scenarios = path_scenarios
        self.path_output = path_output

        assert os.path.exists(self.path_scenarios)
        assert check_parent_exists_folder_remove(path_output, True)

        create_folder(path_output, delete_if_exists=True)

        self.return_df = pd.DataFrame()

        for f in os.listdir(os.path.abspath(path_scenarios)):
            bname = os.path.basename(f)
            if bname.endswith(".scenario"):
                scname = bname.replace(".scenario", "")
                query_list.append({"par_id": scname,
                                   "scenario_path": os.path.join(path_scenarios, f),
                                   "output_path": os.path.join(path_output, "_".join([scname, "output"]))})

                self.return_df.loc[scname, "output_path"] = path_output

        self.scenario_paths = {p["scenario_path"] for p in query_list}

        super(MultiScenarioOutput, self).__init__(query_list, model, qoi=None)

    @classmethod
    def _remote_run(cls, remote_folder_path, remote_output_folder, remote_model_path, remote_pickle_res_path, njobs):
        setup = cls(remote_folder_path, remote_output_folder, remote_model_path)
        res = setup.run(njobs)
        cls.dump_result_pickle(res, remote_pickle_res_path)

    def remote(self, njobs):

        with ServerConnection() as sc:
            self.setup_connection(sc)

            for file in self.scenario_paths:
                self._transfer_local2remote(file)

            remote_model_path = self._transfer_model_local2remote(self.model)

            # TODO provide the default pickle paths in a function
            remote_pickle_res_path = self._join_linux_path([self.remote_folder_path, "result.p"], is_folder=False)

            s = f"""python3 -c 'import suqc; suqc.MultiScenarioOutput._remote_run("{self.remote_folder_path}", "{self.remote_output_folder()}", "{remote_model_path}", "{remote_pickle_res_path}", {njobs})'"""
            self.server.con.run(s)

            # TODO: duplicated code!
            zipped_file_local = self._transfer_compressed_output_remote2local(self.path_output)
            self._uncompress_file2target(zipped_file_local, self.path_output)

            self._transfer_pickle_remote2local(remote_pickle_res_path, os.path.join(self.path_output, "results.p"))
            self._remove_remote_folder()

    def run(self, njobs: int = 1):
        _, req_time = super(MultiScenarioOutput, self).run(njobs)
        self.return_df.loc[:, "req_time"] = req_time
        return self.return_df


class SingleScenarioOutput(Request, ServerRequest):

    def __init__(self, path_scenario, path_output, model, qoi=None, force_replace=False):

        self.path_scenario = path_scenario
        self.path_output = path_output

        if qoi is not None:  # TODO: write "Infer QoI" function
            if isinstance(qoi, (str, list)):
                with open(path_scenario, "r") as f:
                    basis_scenario = json.load(f)

                qoi = QuantityOfInterest(basis_scenario=basis_scenario, requested_files=qoi)
            else:
                raise ValueError("Invalid format of Quantity of Interest")

        path_scenario = os.path.abspath(path_scenario)

        assert os.path.exists(path_scenario) and path_scenario.endswith(".scenario")
        assert check_parent_exists_folder_remove(path_output, not force_replace)

        scname = os.path.basename(path_scenario).replace(".scenario", "")

        query_list = [{"par_id": scname,
                       "scenario_path": path_scenario,
                       "output_path": path_output}]

        super(SingleScenarioOutput, self).__init__(query_list, model, qoi)

    def run(self, njobs: int = 1):
        if njobs != 1:
            print("WARNING: For a single scenario only njobs=1 is valid. Setting njobs=1.")
            njobs=1

        res = super(SingleScenarioOutput, self).run(njobs)
        return res

    @classmethod
    def _remote_run(cls, remote_scenario_path, remote_output_folder, remote_model_path, remote_pickle_arg_path, remote_pickle_res_path):

        if remote_pickle_arg_path is not None and os.path.exists(remote_pickle_arg_path):
            kwargs = cls.open_arg_pickle(remote_pickle_arg_path)
            setup = cls(remote_scenario_path, remote_output_folder, remote_model_path, kwargs["qoi"], True)
            res = setup.run(1)
            cls.dump_result_pickle(res, remote_pickle_res_path)
        else:
            setup = cls(remote_scenario_path, remote_output_folder, remote_model_path, None, True)
            setup.run(1)

    def remote(self):

        with ServerConnection() as sc:
            self.setup_connection(sc)

            # Setup
            remote_scenario_path = self._transfer_local2remote(self.path_scenario)
            remote_model_path = self._transfer_model_local2remote(self.model)

            if self.qoi is not None:
                pickle_content = {"qoi": self.qoi}
                remote_pickle_arg_path = self._transfer_pickle_local2remote(**pickle_content)
                remote_pickle_res_path = self._join_linux_path([self.remote_folder_path, "result.p"], is_folder=False)
                s = f"""python3 -c 'import suqc; suqc.SingleScenarioOutput._remote_run("{remote_scenario_path}", "{self.remote_output_folder()}", "{remote_model_path}", "{remote_pickle_arg_path}", "{remote_pickle_res_path}")'"""
            else:
                remote_pickle_res_path = None
                s = f"""python3 -c 'import suqc; suqc.SingleScenarioOutput._remote_run("{remote_scenario_path}", "{self.remote_output_folder()}", "{remote_model_path}", None, None)'"""

            self.server.con.run(s)

            zipped_file_local = self._transfer_compressed_output_remote2local(self.path_output)
            self._uncompress_file2target(zipped_file_local, self.path_output)

            if self.qoi is not None:
                self._transfer_pickle_remote2local(remote_pickle_res_path, os.path.join(self.path_output, "result.p"))

            self._remove_remote_folder()


if __name__ == "__main__":

    # QuickVaryScenario()



    # par, res = QuickRequest(scenario_path="/home/daniel/REPOS/suq-controller/suqc/rimea_13_stairs_long_nelder_mead.scenario",
    #              parameter_var=[{"speedDistributionMean": 0.1}, {"speedDistributionMean": 0.2}, {"speedDistributionMean": 0.3}],
    #              qoi="postvis.trajectories", model="vadere0_7rc.jar")

    # res = provide_scenarios_run(path_scenarios="/home/daniel/REPOS/suq-controller/suqc/suqc_envs/test_provide_scenario",
    #                       path_output="/home/daniel/REPOS/suq-controller/suqc/suqc_envs/test_provide_scenario",
    # #                       model="vadere0_7rc.jar", njobs=1)
    # res = provide_single_scenario(path_scenario="/home/daniel/Code/vadere/VadereModelTests/TestOSM/scenarios/basic_2_density_discrete_ca.scenario",
    #                               path_output="/home/daniel/test/test01", model="vadere0_7rc.jar")
    # print(res)

    # par, res = single_key_request(scenario_path="/home/daniel/REPOS/suq-controller/suqc/basic_2_density_discrete_ca.scenario",
    #                               key="speedDistributionMean", values=np.array([1.1, 2.1, 2.4]),
    #                               qoi="density.txt",
    #                               model="vadere0_7rc.jar")



    exit()

    # em = EnvironmentManager("corner", model="vadere0_7rc.jar")
    # pv = FullGridSampling()
    # pv.add_dict_grid({"speedDistributionStandardDeviation": [0.0, 0.1, 0.2, 0.3], "speedDistributionMean": [1.2, 1.3]})
    # q0 = QuantityOfInterest("evacuationTimes.txt", em)
    #
    # par_lu, result = Request(em, pv, q0).run(njobs=1)

    exit()

    #q1 = PedestrianDensityGaussianProcessor(em) # TODO: need to check if qoi-processor is available in basis file!
    #
    #
    # pv = BoxSamplingUlamMethod()
    # pv.create_grid("speedDistributionStandardDeviation", 0, 0.5, 3, 3)
    #
    # sc = ScenarioChanges(apply_default=True)
    #
    # q = Query(em, pv, q0, sc).run(njobs=-1)
    # print(q)
    #
    # # q = Query(em, pv, AreaDensityVoronoiProcessor(em)).run(njobs=1)
    # # print(q)
