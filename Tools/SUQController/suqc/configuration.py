#!/usr/bin/env python3

import json
import os
import os.path as p
import pathlib

# configuration of the suq-controller
DEFAULT_SUQC_CONFIG = {
    "default_vadere_src_path": "TODO",  # TODO Feature: #25
    "server": {"host": "", "user": "", "port": -1},
}


def check_setup(_paths_class):

    if (
        not os.path.exists(_paths_class.path_cfg_folder())
        and _paths_class.is_package_paths()
    ):
        print(f"INFO: Setting up configuration folder {_paths_class.path_cfg_folder()}")
        # the next two checks will fail automatically too, because the folder is empty
        os.mkdir(_paths_class.path_cfg_folder())

    if not os.path.exists(_paths_class.path_suq_config_file()):
        print(
            f"INFO: Setting up default configuration file located at "
            f"{_paths_class.path_suq_config_file()}"
        )
        _paths_class.store_config(DEFAULT_SUQC_CONFIG)

    if not os.path.exists(_paths_class.path_container_folder()):
        print(
            f"INFO: Setting up the default container path "
            f"(which will store output of simulation runs). "
            f"Location {_paths_class.path_container_folder()}"
        )
        os.mkdir(_paths_class.path_container_folder())

    return _paths_class


# class annotation -> everythime the class is used, it will be checked if the folders are correctly configured
@check_setup
class SuqcConfig(object):

    NAME_PACKAGE = "suqc"
    NAME_SUQ_CONFIG_FILE = "suq_controller.conf"
    NAME_MODELS_FOLDER = "models"
    NAME_CON_FOLDER = "suqc_envs"
    NAME_PACKAGE_FILE = "PACKAGE.txt"

    IS_PACKAGE: bool = None

    @classmethod
    def is_package_paths(cls):
        if cls.IS_PACKAGE is None:
            return os.path.exists(cls.path_package_indicator_file())
        else:
            return SuqcConfig.IS_PACKAGE

    @classmethod
    def set_package_paths(cls, package: bool):
        SuqcConfig.IS_PACKAGE = package

    @classmethod
    def _name_cfg_folder(cls):
        if cls.is_package_paths():
            return ".config"
        else:
            raise RuntimeError("This should not be called when IS_PACKAGE=False.")

    @classmethod
    def path_usrhome_folder(cls):
        return pathlib.Path.home()

    @classmethod
    def path_src_folder(cls):
        return p.abspath(p.join(p.realpath(__file__), os.pardir))

    @classmethod
    def path_package_indicator_file(cls):
        return p.join(cls.path_src_folder(), cls.NAME_PACKAGE_FILE)

    @classmethod
    def path_cfg_folder(cls):
        if cls.is_package_paths():
            dir2conf = p.join(cls.path_usrhome_folder(), cls._name_cfg_folder())
            if not p.exists(dir2conf):
                # TODO instead of raising error the directory can also be created.
                raise ValueError(f"Directory {dir2conf} does not exist.")
            return dir2conf
        else:
            return cls.path_src_folder()

    @classmethod
    def path_suq_config_file(cls):
        if cls.is_package_paths():
            return p.join(cls.path_cfg_folder(), cls.NAME_SUQ_CONFIG_FILE)
        else:
            return p.join(cls.path_src_folder(), cls.NAME_SUQ_CONFIG_FILE)

    @classmethod
    def load_cfg_file(cls):
        with open(cls.path_suq_config_file(), "r") as f:
            cfg = json.load(f)
        return cfg

    @classmethod
    def path_models_folder(cls):
        if cls.is_package_paths():
            return p.join(cls.path_cfg_folder(), cls.NAME_MODELS_FOLDER)
        else:
            return p.join(cls.path_src_folder(), cls.NAME_MODELS_FOLDER)

    @classmethod
    def path_container_folder(cls):
        if cls.is_package_paths():
            return p.join(cls.path_usrhome_folder(), cls.NAME_CON_FOLDER)
        else:
            return p.join(cls.path_src_folder(), cls.NAME_CON_FOLDER)

    @classmethod
    def store_config(cls, cfg):
        with open(cls.path_suq_config_file(), "w") as outfile:
            json.dump(cfg, outfile, indent=4)

    @classmethod
    def store_server_config(cls, host: str, user: str, port: int):
        cfg_file = cls.load_cfg_file()
        cfg_file["server"]["host"] = host
        cfg_file["server"]["user"] = user
        cfg_file["server"]["port"] = port
        cls.store_config(cfg_file)


if __name__ == "__main__":
    print(SuqcConfig)
