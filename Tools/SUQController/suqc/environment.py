#!/usr/bin/env python3

# TODO: """ << INCLUDE DOCSTRING (one-line or multi-line) >> """

import json
import glob
import subprocess
import time
import os

from shutil import rmtree
from typing import *

from suqc.configuration import SuqcConfig
from suqc.utils.general import user_query_yes_no, get_current_suqc_state, create_folder

# --------------------------------------------------
# people who contributed code
__authors__ = "Daniel Lehmberg"
# people who made suggestions or reported bugs but didn't contribute code
__credits__ = ["n/a"]
# --------------------------------------------------

# configuration of the suq-controller
DEFAULT_SUQ_CONFIG = {"default_vadere_src_path": "TODO",   # TODO Feature: compile Vadere before using the jar file
                      "server": {
                          "host": "",
                          "user": "",
                          "port": -1
                      }}


@DeprecationWarning
def get_suq_config():
    assert os.path.exists(SuqcConfig.path_suq_config_file()), "Config file does not exist."

    with open(SuqcConfig.path_suq_config_file(), "r") as f:
        config_file = f.read()
    return json.loads(config_file)


class VadereConsoleWrapper(object):

    # Current log level choices, requires to manually add, if there are changes
    ALLOWED_LOGLVL = ["OFF", "FATAL", "TOPOGRAPHY_ERROR", "TOPOGRAPHY_WARN", "INFO", "DEBUG", "ALL"]

    def __init__(self, model_path: str, loglvl="ALL"):

        self.jar_path = os.path.abspath(model_path)
        assert os.path.exists(self.jar_path)

        self.loglvl = loglvl

        assert self.loglvl in self.ALLOWED_LOGLVL, f"set loglvl={self.loglvl} not contained in allowed: " \
            f"{self.ALLOWED_LOGLVL}"

        if not os.path.exists(self.jar_path):
            raise FileExistsError(f"Vadere console file {self.jar_path} does not exist.")

    def run_simulation(self, scenario_fp, output_path):
        start = time.time()
        ret_val = subprocess.call(["java", "-jar",
                                   self.jar_path, "--loglevel",
                                   self.loglvl, "suq", "-f", scenario_fp,
                                   "-o", output_path])
        return ret_val, time.time() - start

    @classmethod
    def from_default_models(cls, model):
        if not model.endswith(".jar"):
            model = ".".join([model, "jar"])
        return cls(os.path.join(SuqcConfig.path_models_folder(), model))

    @classmethod
    def from_model_path(cls, model_path):
        return cls(model_path)

    @classmethod
    def from_new_compiled_package(cls, src_path=None):
        pass  # TODO: use default src_path

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
            raise ValueError("Failed to infer the Vadere model.")


class EnvironmentManager(object):

    output_folder = "vadere_output"

    def __init__(self, env_name: str):

        self.name = env_name
        self.env_path = self.environment_path(self.name)

        print(f"INFO: Set environment path to {self.env_path}")
        if not os.path.exists(self.env_path):
            raise FileNotFoundError(f"Environment {self.env_path} does not exist. Use function "
                                    f"'EnvironmentManager.create_environment' or "
                                    f"'EnvironmentManager.create_if_not_exist'")
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
        sc_files = glob.glob(os.path.join(self.env_path, "*.scenario"))
        assert len(sc_files) == 1, "None or too many .scenario files found in environment."
        return sc_files[0]

    @classmethod
    def create_if_not_exist(cls, env_name: str, basis_scenario: Union[str, dict]):
        target_path = cls.environment_path(env_name)
        if os.path.exists(target_path):
            existing = cls(env_name)

            # TODO: maybe it is good to compare if the handled file is the same as the existing
            #exist_basis_file = existing.get_vadere_scenario_basis_file()
            return existing
        else:
            return cls.create_environment(env_name, basis_scenario)

    @classmethod
    def create_environment(cls, env_name: str, basis_scenario: Union[str, dict], replace: bool = False):

        # Check if environment already exists
        target_path = cls.environment_path(env_name)

        if replace and os.path.exists(target_path):
            if replace:
                cls.remove_environment(env_name, force=True)
            elif not cls.remove_environment(env_name):
                print("Aborting to create a new scenario.")
                return

        # Create new environment folder
        os.mkdir(target_path)

        if isinstance(basis_scenario, str):  # assume that this is a path

            assert os.path.isfile(basis_scenario), "Filepath to .scenario does not exist"
            assert basis_scenario.split(".")[-1] == "scenario", "File has to be a Vadere '*.scenario' file"

            with open(basis_scenario, "r") as file:
                basis_scenario = file.read()

        basis_fp = os.path.join(target_path, f"BASIS_{env_name}.scenario")

        # FILL IN THE STANDARD FILES IN THE NEW SCENARIO:
        with open(basis_fp, "w") as file:
            if isinstance(basis_scenario, dict):
                json.dump(basis_scenario, file, indent=4)
            else:
                file.write(basis_scenario)

        # Create and store the configuration file to the new folder
        cfg = dict()

        if not SuqcConfig.is_package_paths():  # TODO it may be good to write the git hash / version number in the file
            cfg["suqc_state"] = get_current_suqc_state()

            with open(os.path.join(target_path, "suqc_commit_hash.json"), 'w') as outfile:
                s = "\n".join(["commit hash at creation", cfg["suqc_state"]["git_hash"]])
                outfile.write(s)

        # Create the folder where the output is stored
        os.mkdir(os.path.join(target_path, EnvironmentManager.output_folder))

        return cls(env_name)

    @classmethod
    def remove_environment(cls, name, force=False):
        target_path = cls.environment_path(name)
        if force or user_query_yes_no(question=f"Are you sure you want to remove the current environment? Path: \n "
        f"{target_path}"):
            try:
                rmtree(target_path)
            except FileNotFoundError:
                print(f"INFO: Tried to remove environment {name}, but did not exist.")
            return True
        return False

    @staticmethod
    def environment_path(name):
        path = os.path.join(SuqcConfig.path_container_folder(), name)
        return path

    def get_env_outputfolder_path(self):
        rel_path = os.path.join(self.env_path, EnvironmentManager.output_folder)
        return os.path.abspath(rel_path)

    def get_par_id_output_path(self, par_id, create):
        scenario_filename = self._scenario_variation_filename(par_id=par_id)
        scenario_filename = scenario_filename.replace(".scenario", "")
        output_path = os.path.join(self.get_env_outputfolder_path(), "".join([scenario_filename, "_output"]))
        if create:
            create_folder(output_path)
        return output_path

    def _scenario_variation_filename(self, par_id):
        return "".join([str(par_id).zfill(10), ".scenario"])

    def scenario_variation_path(self, par_id):
        return os.path.join(self.get_env_outputfolder_path(), self._scenario_variation_filename(par_id))

    def save_scenario_variation(self, par_id, content):
        fp = self.scenario_variation_path(par_id)
        assert not os.path.exists(fp), f"File {fp} already exists!"

        with open(fp, "w") as outfile:
            json.dump(content, outfile, indent=4)
        return fp