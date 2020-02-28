#!/usr/bin/env python3

import json
import glob
import subprocess
import time
import os

from shutil import rmtree
from typing import *

from suqc.configuration import SuqcConfig
from suqc.utils.general import user_query_yes_no, get_current_suqc_state, str_timestamp


# configuration of the suq-controller
DEFAULT_SUQ_CONFIG = {"default_vadere_src_path": "TODO",
                      "server": {
                          "host": "",
                          "user": "",
                          "port": -1
                      }}


class VadereConsoleWrapper(object):

    # Current log level choices, requires to manually add, if there are changes
    ALLOWED_LOGLVL = ["OFF", "FATAL", "TOPOGRAPHY_ERROR", "TOPOGRAPHY_WARN", "INFO", "DEBUG", "ALL"]

    def __init__(self, model_path: str, loglvl="INFO", jvm_flags: Optional[List] =
    None, timeout_sec=None):

        self.jar_path = os.path.abspath(model_path)

        if not os.path.exists(self.jar_path):
            raise FileNotFoundError(
                f"Vadere console .jar file {self.jar_path} does not exist.")

        loglvl = loglvl.upper()
        if loglvl not in self.ALLOWED_LOGLVL:
            raise ValueError(f"set loglvl={loglvl} not contained "
                             f"in allowed: {self.ALLOWED_LOGLVL}")

        if jvm_flags is not None and not isinstance(jvm_flags, list):
            raise TypeError(
                f"jvm_flags are required to be a list. Got: {type(jvm_flags)}")

        if timeout_sec is None:
            pass # do nothing, no timeout
        elif not isinstance(timeout_sec, int) or timeout_sec <= 0:
            raise TypeError("vadere_run_timeout_sec must be of type int and positive "
                            "value")

        self.loglvl = loglvl
        # Additional Java Virtual Machine options / flags
        self.jvm_flags = jvm_flags if jvm_flags is not None else []
        self.timeout_sec = timeout_sec

    def run_simulation(self, scenario_fp, output_path):
        start = time.time()

        subprocess_cmd = ["java"]
        subprocess_cmd += self.jvm_flags
        subprocess_cmd += ["-jar", self.jar_path]
        # Vadere console commands
        subprocess_cmd += ["--loglevel", self.loglvl]
        subprocess_cmd += ["suq", "-f", scenario_fp, "-o", output_path]

        output_subprocess = dict()

        try:
            subprocess.check_output(subprocess_cmd,
                                    timeout=self.timeout_sec,
                                    stderr=subprocess.PIPE)
            process_duration = time.time() - start

            # if return_code != 0 a subprocess.CalledProcessError is raised
            return_code = 0
            output_subprocess = None
        except subprocess.TimeoutExpired as exception:
            return_code = 1
            process_duration = self.timeout_sec
            output_subprocess["stdout"] = exception.stdout
            output_subprocess["stderr"] = None
        except subprocess.CalledProcessError as exception:
            return_code = exception.returncode
            process_duration = time.time() - start
            output_subprocess["stdout"] = exception.stdout
            output_subprocess["stderr"] = exception.stderr

        return return_code, process_duration, output_subprocess

    @classmethod
    def from_default_models(cls, model):
        if not model.endswith(".jar"):
            model = ".".join([model, "jar"])
        return cls(os.path.join(SuqcConfig.path_models_folder(), model))

    @classmethod
    def from_model_path(cls, model_path):
        return cls(model_path)

    @classmethod
    def infer_model(cls, model):
        if isinstance(model, str):
            if os.path.exists(model):
                return VadereConsoleWrapper.from_model_path(os.path.abspath(model))
            else:
                return VadereConsoleWrapper.from_default_models(model)
        elif isinstance(model, VadereConsoleWrapper):
            return model
        else:
            raise ValueError(f"Failed to infer Vadere model. \n {model}")


class EnvironmentManager(object):

    PREFIX_BASIS_SCENARIO = "BASIS_"
    VADERE_SCENARIO_FILE_TYPE = ".scenario"
    vadere_output_folder = "vadere_output"

    def __init__(self, base_path, env_name: str):

        self.base_path, self.env_name = self.handle_path_and_env_input(base_path, env_name)

        self.env_name = env_name
        self.env_path = self.output_folder_path(self.base_path, self.env_name)

        # output is usually of the following format:
        # 000001_000002 for variation 1 and run_id 2
        # Change these attributes externally, if less digits are required to have
        # shorter/longer paths.
        self.nr_digits_variation = 6
        self.nr_digits_runs = 6

        print(f"INFO: Set environment path to {self.env_path}")
        if not os.path.exists(self.env_path):
            raise FileNotFoundError(f"Environment {self.env_path} does not exist. Use function "
                                    f"'EnvironmentManager.create_new_environment'")
        self._scenario_basis = None

    @property
    def basis_scenario(self):
        if self._scenario_basis is None:
            path_basis_scenario = self.path_basis_scenario

            with open(path_basis_scenario, "r") as f:
                basis_file = json.load(f)
            self._scenario_basis = basis_file

        return self._scenario_basis

    @property
    def path_basis_scenario(self):
        sc_files = glob.glob(os.path.join(self.env_path, f"*{self.VADERE_SCENARIO_FILE_TYPE}"))

        if len(sc_files) != 1:
            raise RuntimeError(f"None or too many '{self.VADERE_SCENARIO_FILE_TYPE}' files "
                               "found in environment.")
        return sc_files[0]

    @classmethod
    def from_full_path(cls, env_path):
        assert os.path.isdir(env_path)
        base_path = os.path.dirname(env_path)

        if env_path.endswith(os.pathsep):
            env_path = env_path.rstrip(os.path.sep)
        env_name = os.path.basename(env_path)

        cls(base_path=base_path, env_name=env_name)

    @classmethod
    def create_new_environment(cls, base_path=None, env_name=None, handle_existing="ask_user_replace"):

        base_path, env_name = cls.handle_path_and_env_input(base_path, env_name)

        # TODO: Refactor, make handle_existing an Enum
        assert handle_existing in ["ask_user_replace", "force_replace", "write_in_if_exist_else_create", "write_in"]

        # set to True if env already exists, and it shouldn't be overwritten
        about_creating_env = False
        env_man = None

        env_exists = os.path.exists(cls.output_folder_path(base_path, env_name))

        if handle_existing == "ask_user_replace" and env_exists:
            if not cls.remove_environment(base_path, env_name):
                about_creating_env = True
        elif handle_existing == "force_replace" and env_exists:
            if env_exists:
                cls.remove_environment(base_path, env_name, force=True)
        elif handle_existing == "write_in":
            assert env_exists, f"base_path={base_path} env_name={env_name} does not exist"
            env_man = cls(base_path=base_path, env_name=env_name)
        elif handle_existing == "write_in_if_exist_else_create":
            if env_exists:
                env_man = cls(base_path=base_path, env_name=env_name)

        if about_creating_env:
            raise ValueError("Could not create new environment.")

        if env_man is None:
            # Create new environment folder
            os.mkdir(cls.output_folder_path(base_path, env_name))
            env_man = cls(base_path=base_path, env_name=env_name)

        return env_man


    @classmethod
    def create_variation_env(cls, basis_scenario: Union[str, dict], base_path=None, env_name=None,
                             handle_existing="ask_user_replace"):

        # Check if environment already exists
        env_man = cls.create_new_environment(base_path=base_path, env_name=env_name, handle_existing=handle_existing)
        path_output_folder = env_man.env_path

        # Add basis scenario used for the variation (i.e. sampling)
        if isinstance(basis_scenario, str):  # assume that this is a path
            if not os.path.isfile(basis_scenario):
                raise FileExistsError("Filepath to .scenario does not exist")
            elif basis_scenario.split(".")[-1] != cls.VADERE_SCENARIO_FILE_TYPE[1:]:
                raise ValueError("basis_scenario has to be a Vadere '*"
                                 f"{cls.VADERE_SCENARIO_FILE_TYPE}' file")

            with open(basis_scenario, "r") as file:
                basis_scenario = file.read()

        # add prefix to scenario file:
        basis_fp = os.path.join(path_output_folder,
                                f"{cls.PREFIX_BASIS_SCENARIO}{env_name}.scenario")

        # FILL IN THE STANDARD FILES IN THE NEW SCENARIO:
        with open(basis_fp, "w") as file:
            if isinstance(basis_scenario, dict):
                json.dump(basis_scenario, file, indent=4)
            else:
                file.write(basis_scenario)

        # Create and store the configuration file to the new folder
        cfg = dict()

        # TODO it may be good to write the git hash / version number in the file
        if not SuqcConfig.is_package_paths():
            cfg["suqc_state"] = get_current_suqc_state()

            with open(os.path.join(path_output_folder, "suqc_commit_hash.json"), 'w') as outfile:
                s = "\n".join(["commit hash at creation", cfg["suqc_state"]["git_hash"]])
                outfile.write(s)

        # Create the folder where all output is stored
        os.mkdir(os.path.join(path_output_folder, EnvironmentManager.vadere_output_folder))

        return cls(base_path, env_name)

    @classmethod
    def remove_environment(cls, base_path, name, force=False):
        target_path = cls.output_folder_path(base_path, name)

        if force or user_query_yes_no(question=f"Are you sure you want to remove the current environment? Path: \n "
        f"{target_path}"):
            try:
                rmtree(target_path)
            except FileNotFoundError:
                print(f"INFO: Tried to remove environment {name}, but did not exist.")
            return True
        return False

    @staticmethod
    def handle_path_and_env_input(base_path, env_name):
        if env_name is None:
            env_name = "_".join(["output", str_timestamp()])

        if base_path is None:
            base_path = SuqcConfig.path_container_folder()

        return base_path, env_name

    @staticmethod
    def output_folder_path(base_path, env_name):
        base_path, env_name = EnvironmentManager.handle_path_and_env_input(base_path, env_name)
        assert os.path.isdir(base_path)
        output_folder_path = os.path.join(base_path, env_name)
        return output_folder_path

    def get_env_outputfolder_path(self):
        rel_path = os.path.join(self.env_path, EnvironmentManager.vadere_output_folder)
        return os.path.abspath(rel_path)

    def get_variation_output_folder(self, parameter_id, run_id):
        scenario_filename = self._scenario_variation_filename(parameter_id=parameter_id, run_id=run_id)
        scenario_filename = scenario_filename.replace(self.VADERE_SCENARIO_FILE_TYPE, "")
        return os.path.join(self.get_env_outputfolder_path(), "".join([scenario_filename, "_output"]))

    def _scenario_variation_filename(self, parameter_id, run_id):
        digits_parameter_id = str(parameter_id).zfill(self.nr_digits_variation)
        digits_run_id = str(run_id).zfill(self.nr_digits_variation)
        numbered_scenario_name = "_".join([digits_parameter_id, digits_run_id])

        return "".join([numbered_scenario_name, self.VADERE_SCENARIO_FILE_TYPE])

    def scenario_variation_path(self, par_id, run_id):
        return os.path.join(self.get_env_outputfolder_path(), self._scenario_variation_filename(par_id, run_id))

    def save_scenario_variation(self, par_id, run_id, content):
        scenario_path = self.scenario_variation_path(par_id, run_id)
        assert not os.path.exists(scenario_path), f"File {scenario_path} already exists!"

        with open(scenario_path, "w") as outfile:
            json.dump(content, outfile, indent=4)
        return scenario_path