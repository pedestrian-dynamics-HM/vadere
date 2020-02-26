#!/usr/bin/env python3

import json
import multiprocessing
import os
import shutil

from suqc.environment import VadereConsoleWrapper
from suqc.parameter.create import VadereScenarioCreation
from suqc.parameter.postchanges import PostScenarioChangesBase
from suqc.parameter.sampling import *
from suqc.qoi import QuantityOfInterest
from suqc.remote import ServerRequest
from suqc.utils.general import create_folder, njobs_check_and_set, parent_folder_clean


class RequestItem(object):

    def __init__(self, parameter_id, run_id, scenario_path, base_path, output_folder):
        self.parameter_id = parameter_id
        self.run_id = run_id
        self.base_path = base_path
        self.output_folder = output_folder
        self.scenario_path = scenario_path

        self.output_path = os.path.join(self.base_path, self.output_folder)

    def add_qoi_result(self, qoi_result):
        self.qoi_result = qoi_result

    def add_meta_info(self, required_time, return_code):
        self.required_time = required_time
        self.return_code = return_code


class Request(object):

    PARAMETER_ID = "id"
    RUN_ID = "run_id"

    def __init__(self, request_item_list: List[RequestItem], model: Union[str, VadereConsoleWrapper],
                 qoi: Union[QuantityOfInterest, None]):

        if len(request_item_list) == 0:
            raise ValueError("request_item_list has no entries.")

        self.model = VadereConsoleWrapper.infer_model(model)
        self.request_item_list = request_item_list
        # Can be None, if this is the case, no output data will be parsed to pd.DataFrame
        self.qoi = qoi

        # Return values as pd.DataFrame from all runs (they cannot be included directly by the runs,
        # because Python's mulitprocessing is not shared memory due to the GIL (i.e. different/independent processes
        # are created
        self.compiled_qoi_data = None
        self.compiled_run_info = None

    def _interpret_return_value(self, ret_val, par_id):
        if ret_val == 0:
            return True
        else:  # ret_val != 0
            print(f"WARNING: Simulation with parameter setting {par_id} failed.")
            return False

    def _single_request(self, request_item: RequestItem) -> RequestItem:

        self._create_output_path(request_item.output_path)

        return_code, required_time, output_on_error = \
            self.model.run_simulation(request_item.scenario_path, request_item.output_path)

        is_results = self._interpret_return_value(return_code, request_item.parameter_id)

        if is_results and self.qoi is not None:
            result = self.qoi.read_and_extract_qois(par_id=request_item.parameter_id, run_id=request_item.run_id,
                                                    output_path=request_item.output_path)
        elif not is_results and self.qoi is not None:
            # something went wrong during run
            assert output_on_error is not None

            filename_stdout = "stdout_on_error.txt"
            filename_stderr = "stderr_on_error.txt"
            self._write_console_output(output_on_error["stdout"],
                                       request_item.output_path,
                                       filename_stdout)
            self._write_console_output(output_on_error["stderr"],
                                       request_item.output_path,
                                       filename_stderr)
            result = None
        else:
            result = None

        if self.qoi is not None and not is_results:
            required_time = np.nan

        request_item.add_qoi_result(result)
        request_item.add_meta_info(required_time=required_time, return_code=return_code)

        # Because of the multi-processor part, don't try to already add the results here to _results_df
        return request_item

    def _create_output_path(self, output_path):
        create_folder(output_path, delete_if_exists=True)
        return output_path

    def _write_console_output(self, msg, output_path, filename):
        _file = os.path.abspath(os.path.join(output_path, filename))

        if msg is not None:
            with open(_file, "wb") as out:
                out.write(msg)

    def _compile_qoi(self):

        qoi_results = [item_.qoi_result for item_ in self.request_item_list]

        filenames = None
        for ires in qoi_results:
            if ires is not None:
                # it is assumed that the the keys for all elements in results are the same!
                # TODO: this assumption may fail... possibly better to check for this!
                filenames = list(ires.keys())
                break

        if filenames is None:
            print("WARNING: All simulations failed, only 'None' results. Look in the "
                  "output folder for error messages.")
            final_results = None
        else:
            # Successful runs are collected and are concatenated into a single pd.DataFrame below
            final_results = dict()

            for filename in filenames:

                collected_df = [item_[filename] for item_ in qoi_results if item_ is not None]
                collected_df = pd.concat(collected_df, axis=0)

                final_results[filename] = collected_df

        if filenames is not None and len(filenames) == 1:
            # There is no need to have the key/value if only one file was requested.
            final_results = final_results[filenames[0]]

        return final_results

    def _compile_run_info(self):
        data = [(item_.parameter_id, item_.run_id, item_.required_time, item_.return_code) for item_ in self.request_item_list]
        df = pd.DataFrame(data, columns=[self.PARAMETER_ID, self.RUN_ID, "required_wallclock_time", "return_code"])
        df.set_index(keys=[self.PARAMETER_ID, self.RUN_ID], inplace=True)
        return df

    def _sp_query(self):
        # enumerate returns tuple(par_id, scenario filepath) see ParameterVariation.generate_vadere_scenarios and
        # ParameterVariation._vars_object()
        for i, request_item in enumerate(self.request_item_list):
            self.request_item_list[i] = self._single_request(request_item)

    def _mp_query(self, njobs):
        pool = multiprocessing.Pool(processes=njobs)
        self.request_item_list = pool.map(self._single_request, self.request_item_list)

    def run(self, njobs: int = 1):

        # nr of rows = nr of parameter settings = #simulations
        nr_simulations = len(self.request_item_list)
        njobs = njobs_check_and_set(njobs=njobs, ntasks=nr_simulations)

        if njobs == 1:
            self._sp_query()
        else:
            self._mp_query(njobs=njobs)

        if self.qoi is not None:
            self.compiled_qoi_data = self._compile_qoi()

        self.compiled_run_info = self._compile_run_info()
        return self.compiled_qoi_data, self.compiled_run_info


class VariationBase(Request, ServerRequest):

    def __init__(self,
                 env_man: EnvironmentManager,
                 parameter_variation: ParameterVariationBase,
                 model: str,
                 qoi: Union[str, List[str], QuantityOfInterest],
                 post_changes: PostScenarioChangesBase = None,
                 njobs: int = 1,
                 remove_output=False):

        self.parameter_variation = parameter_variation
        self.env_man = env_man
        self.post_changes = post_changes
        self.model = model
        self.remove_output = remove_output

        if qoi is None and remove_output:
            raise ValueError("it does not make sense: not collecting a qoi (qoi=None and "
                             "not keeping the output (remove_output=False).")

        if isinstance(qoi, (str, list)):
            self.qoi = QuantityOfInterest(basis_scenario=self.env_man.basis_scenario, requested_files=qoi)
        else:
            self.qoi = qoi

        scenario_creation = VadereScenarioCreation(self.env_man, self.parameter_variation, self.post_changes)
        request_item_list = scenario_creation.generate_vadere_scenarios(njobs)

        super(VariationBase, self).__init__(request_item_list, self.model, self.qoi)
        ServerRequest.__init__(self)

    def _remove_output(self):
        if self.env_man.env_path is not None:
            shutil.rmtree(self.env_man.env_path)

    def run(self, njobs: int = 1):
        qoi_result_df, meta_info = super(VariationBase, self).run(njobs)

        # add another level to distinguish the columns with the parameter lookup
        meta_info.columns = pd.MultiIndex.from_arrays([["MetaInfo"] * meta_info.shape[1], meta_info.columns])
        lookup_df = pd.concat([self.parameter_variation.points, meta_info], axis=1)

        if self.remove_output:
            self._remove_output()

        return lookup_df, qoi_result_df

    @classmethod
    def _remote_run(cls, remote_pickle_arg_path):

        kwargs = cls.open_arg_pickle(remote_pickle_arg_path)
        env_man = EnvironmentManager(base_path=None, env_name=kwargs["remote_env_name"])

        setup = cls(env_man=env_man,
                    parameter_variation=kwargs["parameter_variation"],
                    model=kwargs["model"],
                    qoi=kwargs["qoi"],
                    post_changes=kwargs["post_changes"],
                    njobs=kwargs["njobs"],
                    remove_output=False)  # the output for remote will be removed after all is transferred

        res = setup.run(kwargs["njobs"])
        cls.dump_result_pickle(res, kwargs["remote_pickle_res_path"])

    def remote(self, njobs=1):
        pickle_content = {"qoi": self.qoi,
                          "parameter_variation": self.parameter_variation,
                          "post_changes": self.post_changes,
                          "njobs": njobs}

        local_transfer_files = {"path_basis_scenario": self.env_man.path_basis_scenario}

        remote_result = super(VariationBase, self)._remote_ssh_logic(local_env_man=self.env_man,
                                                                     local_pickle_content=pickle_content,
                                                                     local_transfer_files=local_transfer_files,
                                                                     local_model_obj=self.model,
                                                                     class_name="VariationBase",
                                                                     transfer_output=not self.remove_output)
        return remote_result


@DeprecationWarning
class SampleVariation(VariationBase, ServerRequest):

    def __init__(self,
                 scenario_path: str,
                 parameter_sampling: ParameterVariationBase,
                 qoi: Union[str, List[str]],
                 model: Union[str, VadereConsoleWrapper],
                 scenario_runs=1,
                 output_path=None,
                 output_folder=None,
                 remove_output=False,
                 env_remote=None):
        # TODO
        pass


class DictVariation(VariationBase, ServerRequest):

    def __init__(self,
                 scenario_path: str,
                 parameter_dict_list: List[dict],
                 qoi: Union[str, List[str]],
                 model: Union[str, VadereConsoleWrapper],
                 scenario_runs=1,
                 post_changes=PostScenarioChangesBase(apply_default=True),
                 njobs_create_scenarios=1,
                 output_path=None,
                 output_folder=None,
                 remove_output=False,
                 env_remote=None):

        self.scenario_path = scenario_path
        self.remove_output = remove_output

        assert os.path.exists(scenario_path) and scenario_path.endswith(".scenario"), \
            "Filepath must exist and the file has to end with .scenario"

        if env_remote is None:
            env = EnvironmentManager.create_variation_env(basis_scenario=self.scenario_path,
                                                          base_path=output_path,
                                                          env_name=output_folder,
                                                          handle_existing="ask_user_replace")
            self.env_path = env.env_path
        else:
            self.env_path = env_remote.env_path
            self.remove_output = False  # Do not remove the folder because this is done with the remote procedure
            env = env_remote

        parameter_variation = UserDefinedSampling(parameter_dict_list)
        parameter_variation = parameter_variation.multiply_scenario_runs(scenario_runs=scenario_runs)

        super(DictVariation, self).__init__(env_man=env,
                                            parameter_variation=parameter_variation,
                                            model=model,
                                            qoi=qoi,
                                            post_changes=post_changes,
                                            njobs=njobs_create_scenarios,
                                            remove_output=remove_output)


class SingleKeyVariation(DictVariation, ServerRequest):

    def __init__(self, scenario_path: str,
                 key: str,
                 values: np.ndarray,
                 qoi: Union[str, List[str]],
                 model: Union[str, VadereConsoleWrapper],
                 scenario_runs=1,
                 post_changes=PostScenarioChangesBase(apply_default=True),
                 output_path=None,
                 output_folder=None,
                 remove_output=False,
                 env_remote=None):

        self.key = key
        self.values = values

        simple_grid = [{key: v} for v in values]
        super(SingleKeyVariation, self).__init__(scenario_path=scenario_path,
                                                 parameter_dict_list=simple_grid,
                                                 qoi=qoi,
                                                 model=model,
                                                 scenario_runs=scenario_runs,
                                                 post_changes=post_changes,
                                                 output_folder=output_folder,
                                                 output_path=output_path,
                                                 remove_output=remove_output,
                                                 env_remote=env_remote)


class FolderExistScenarios(Request, ServerRequest):

    def __init__(self, path_scenario_folder, model, scenario_runs=1, output_path=None,
                 output_folder=None,
                 handle_existing="ask_user_replace"):

        self.scenario_runs = scenario_runs
        assert os.path.exists(path_scenario_folder)
        self.path_scenario_folder = path_scenario_folder

        self.env_man = EnvironmentManager.create_new_environment(base_path=output_path, env_name=output_folder,
                                                                 handle_existing=handle_existing)

        request_item_list = list()

        for filename in os.listdir(os.path.abspath(path_scenario_folder)):
            file_base_name = os.path.basename(filename)

            if file_base_name.endswith(".scenario"):
                scenario_name = file_base_name.replace(".scenario", "")

                request_item = self._generate_request_items(
                    scenario_name=scenario_name, filename=filename)

                request_item_list += request_item

        super(FolderExistScenarios, self).__init__(request_item_list=request_item_list,
                                                   model=model,
                                                   qoi=None)
        ServerRequest.__init__(self)

    @classmethod
    def _remote_run(cls, remote_pickle_arg_path):

        kwargs = cls.open_arg_pickle(remote_pickle_arg_path)

        setup = cls(path_scenario_folder=kwargs["remote_folder_path"], model=kwargs["model"],
                    output_folder=kwargs["remote_env_name"], handle_existing="write_in")
        res = setup.run(kwargs["njobs"])
        cls.dump_result_pickle(res, kwargs["remote_pickle_res_path"])

    def _generate_request_items(self, scenario_name, filename):

        # generate request item for each scenario run
        scenario_request_items = list()
        for run in range(self.scenario_runs):

            item = RequestItem(parameter_id=scenario_name,
                               run_id=run,
                               scenario_path=os.path.join(self.path_scenario_folder, filename),
                               base_path=self.env_man.env_path,
                               output_folder="_".join([scenario_name, "output",
                                                       "" if self.scenario_runs == 1
                                                       else str(run)]))

            scenario_request_items.append(item)
        return scenario_request_items

    def remote(self, njobs=1):

        local_pickle_content = {"njobs": njobs}

        local_transfer_files = dict()
        for i, request in enumerate(self.request_item_list):
            local_transfer_files["scenario_path_{i}"] = request.scenario_path

        self._remote_ssh_logic(local_env_man=self.env_man,
                               local_pickle_content=local_pickle_content,
                               local_transfer_files=local_transfer_files,
                               local_model_obj=self.model,
                               class_name="FolderExistScenarios",
                               transfer_output=True)

    def run(self, njobs: int = 1):
        _, meta_info = super(FolderExistScenarios, self).run(njobs)
        return meta_info


class ProjectOutput(FolderExistScenarios):

    def __init__(self, project_path, model):

        if not os.path.exists(project_path):
            raise ValueError(f"project_path {project_path} odes not exist.")

        if not os.path.isfile(project_path) or not project_path.endswith(".project"):
            raise ValueError(f"project_path has to be the path to a Vadere project file (ending with .project).")

        parent_path = parent_folder_clean(project_path)

        # This is by Vaderes convention:
        path_scenario_folder = os.path.join(parent_path, "scenarios")
        super(ProjectOutput, self).__init__(path_scenario_folder=path_scenario_folder,
                                            model=model,
                                            output_path=parent_path,
                                            output_folder="output")


class SingleExistScenario(Request, ServerRequest):

    def __init__(self, path_scenario,
                 qoi,
                 model,
                 scenario_runs=1,
                 output_path=None,
                 output_folder=None,
                 handle_existing="ask_user_replace"):

        self.path_scenario = os.path.abspath(path_scenario)
        assert os.path.exists(self.path_scenario) and self.path_scenario.endswith(".scenario")

        scenario_name = os.path.basename(path_scenario).replace(".scenario", "")

        self.env_man = EnvironmentManager.create_new_environment(base_path=output_path,
                                                                 env_name=output_folder,
                                                                 handle_existing=handle_existing)
        self.scenario_runs = scenario_runs

        if qoi is not None:
            if isinstance(qoi, (str, list)):
                with open(path_scenario, "r") as f:
                    basis_scenario = json.load(f)
                qoi = QuantityOfInterest(basis_scenario=basis_scenario, requested_files=qoi)
            else:
                raise ValueError("Invalid format of Quantity of Interest")

        request_item_list = self._generate_request_list(scenario_name=scenario_name, path_scenario=path_scenario)

        super(SingleExistScenario, self).__init__(request_item_list=request_item_list,
                                                  model=model,
                                                  qoi=qoi)
        ServerRequest.__init__(self)

    def _generate_request_list(self, scenario_name, path_scenario):

        if self.scenario_runs == 1:
            # No need to attach the run_id if there is only one run
            output_folder = lambda run_id: os.path.join(self.env_man.env_name, "vadere_output")
        else:
            output_folder = lambda run_id: os.path.join(self.env_man.env_name,
                                                        f"vadere_output_{str(run_id).zfill(len(str(run_id)))}")

        request_item_list = list()
        for run_id in range(self.scenario_runs):
            request_item = RequestItem(parameter_id=scenario_name,
                                       run_id=run_id,
                                       scenario_path=path_scenario,
                                       base_path=self.env_man.base_path,
                                       output_folder=output_folder(run_id=run_id))
            request_item_list.append(request_item)
        return request_item_list

    def run(self, njobs: int = 1):
        res = super(SingleExistScenario, self).run(njobs)
        return res

    @classmethod
    def _remote_run(cls, remote_pickle_arg_path):

        kwargs = cls.open_arg_pickle(remote_pickle_arg_path)

        setup = cls(path_scenario=kwargs["path_scenario"],
                    qoi=kwargs["qoi"],
                    model=kwargs["model"],
                    scenario_runs=kwargs["scenario_runs"],
                    output_path=None,
                    output_folder=kwargs["remote_env_name"],
                    handle_existing="write_in")  # needs to write in because the environment already exists

        res = setup.run(njobs=kwargs["njobs"])
        cls.dump_result_pickle(res, kwargs["remote_pickle_res_path"])

    def remote(self, njobs=1):

        local_pickle_content = {"njobs": njobs, "qoi": self.qoi, "scenario_runs": self.scenario_runs}
        local_transfer_files = {"path_scenario": self.path_scenario}

        self._remote_ssh_logic(local_env_man=self.env_man,
                               local_pickle_content=local_pickle_content,
                               local_transfer_files=local_transfer_files,
                               local_model_obj=self.model,
                               class_name="SingleExistScenario",
                               transfer_output=True)


if __name__ == "__main__":
    pass
