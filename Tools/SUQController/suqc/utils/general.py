#!/usr/bin/env python3

# TODO: """ << INCLUDE DOCSTRING (one-line or multi-line) >> """

import datetime
import multiprocessing
import os
import shutil
import subprocess
import sys
from typing import Union

import pandas as pd

import suqc
from suqc.configuration import SuqcConfig
from fnmatch import fnmatch, filter


def get_current_suqc_state():
    # message only for developer, -- if the installed package is running
    if not SuqcConfig.is_package_paths():
        GIT_COMMIT_HASH = subprocess.check_output(["git", "rev-parse", "HEAD"])
        GIT_COMMIT_HASH = GIT_COMMIT_HASH.decode().strip()

        uncommited_changes = subprocess.check_output(["git", "status", "--porcelain"])
        uncommited_changes = (
            uncommited_changes.decode()
        )  # is returned as a byte sequence -> decode to string

        if uncommited_changes:
            print("WARNING: THERE ARE UNCOMMITED CHANGED IN THE REPO")
            print(
                "In order to have a reproducible scenario run you should check if untracked changes in the following "
                "files should be commited before: \n"
            )
            print(uncommited_changes)

        return {"git_hash": GIT_COMMIT_HASH, "umcommited_changes": uncommited_changes}
    else:
        return {"suqc_version": suqc.__version__}


def cast_series_if_possible(data: Union[pd.DataFrame, pd.Series]):
    if isinstance(data, pd.Series):
        return data  # already Series, nothing to do

    if isinstance(data, pd.DataFrame) and data.shape[1] == 1:
        return data.iloc[:, 0]
    else:
        return data  # not a Series, remains a DataFrame


def create_folder(path, delete_if_exists=True):
    if delete_if_exists and os.path.exists(path):
        remove_folder(path)
    os.mkdir(path)


def check_parent_exists_folder_remove(folder_path, ask_user_to_replace: bool):
    if folder_path.endswith("/"):
        folder_path = folder_path.rstrip("/")

    # parent folder:
    parent = os.path.dirname(folder_path)

    if not os.path.exists(parent) or not os.path.isdir(parent):
        raise ValueError(
            f"Path {folder_path} is not valid, because path {parent} is not a directory or does "
            f"not exist."
        )

    if os.path.exists(folder_path):
        if os.path.isfile(folder_path):
            raise ValueError(f"Path to {folder_path} is a file not a directory.")

        if ask_user_to_replace and not user_query_yes_no(
            f"The directory {folder_path} does already exist. In the process it may get removed (which results in "
            f"a loss of data). Do you want to proceed?",
            default="no",
        ):
            print("Terminating...")
            exit()

    return True


def njobs_check_and_set(njobs, ntasks):
    nkernels = multiprocessing.cpu_count()

    if not isinstance(njobs, int) or njobs == 0 or njobs < -1:
        raise ValueError(
            "njobs has to be an integer and cannot be zero or smaller than -1"
        )

    if njobs > ntasks:
        print(
            f"WARNING: More jobs are requested (={njobs}) than tasks to carry out (={ntasks}). "
            f"Setting njobs={ntasks}."
        )
        return ntasks

    if njobs > nkernels:
        print(
            f"WARNING: More jobs (={njobs}) requested than CPU kernels available (={nkernels}). "
        )
        return njobs

    if njobs == -1:  # this is adapted to scikit-learn way
        njobs = min(nkernels, ntasks)
        print(
            f"INFO: Available cpus: {nkernels}. Nr. of tasks {ntasks}. "
            f"Setting to njobs={njobs}."
        )
        return njobs

    return njobs


def str_timestamp():
    return datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")


def parent_folder_clean(p):
    # problem that is solved here:
    # os.path.dirname(/home/user) = /home
    # os.path.dirname(/home/user/) = /home/user
    # so the trailing '/' has an effect, but it is often not consistent to denote directories with a trailing '/'

    if p.endswith("/"):  # in case we want the parent folder of a directory
        p = p.rstrip("/")

    parent = os.path.dirname(p)
    return parent + os.path.sep  # better to indicate a folder with trailing


def remove_folder(path):
    if os.path.exists(path):
        shutil.rmtree(path)


def user_query_yes_no(question: str, default=None) -> bool:
    """Ask a yes/no question via raw_input() and return their answer.

    "question" is a string that is presented to the user.
    "default" is the presumed answer if the user just hits <Enter>.
        It must be "yes" (the default), "no" or None (meaning
        an answer is required of the user).

    The "answer" return value is True for "yes" or False for "no".
    """

    # source: https://stackoverflow.com/questions/3041986/apt-command-line-interface-like-yes-no-input

    valid = {"yes": True, "y": True, "ye": True, "no": False, "n": False}
    if default is None:
        prompt = " [y/n] "
    elif default == "yes":
        prompt = " [Y/n] "
    elif default == "no":
        prompt = " [y/N] "
    else:
        raise ValueError("invalid default answer: '%s'" % default)

    while True:
        sys.stdout.write(question + prompt)
        sys.stdout.flush()
        choice = input().lower()
        sys.stdout.write("\n")
        sys.stdout.flush()

        if default is not None and choice == "":
            return valid[default]
        elif choice in valid:
            return valid[choice]
        else:
            print("Please respond with 'yes' or 'no' (or 'y' or 'n').")


def user_query_numbered_list(elements: list):

    max_choice = len(elements) - 1
    print("Choose an option of the following list:")

    for i, txt in enumerate(elements):
        print(f"{i} \t {txt}")

    while True:
        print(f"Type in a number between 0 and {max_choice}")
        choice = input().lower()
        try:
            choice = int(choice)
            if 0 <= choice <= max_choice:
                return elements[choice]
        except ValueError:
            print("The number has to be an integer.")


def include_patterns(*patterns):

    # https://stackoverflow.com/questions/42487578/python-shutil-copytree-use-ignore-function-to-keep-specific-files-types

    """Factory function that can be used with copytree() ignore parameter.

    Arguments define a sequence of glob-style patterns
    that are used to specify what files to NOT ignore.
    Creates and returns a function that determines this for each directory
    in the file hierarchy rooted at the source directory when used with
    shutil.copytree().
    """

    def _ignore_patterns(path, names):

        keep = set(name for pattern in patterns for name in filter(names, pattern))
        ignore = set(
            name
            for name in names
            if name not in keep and not os.path.isdir(os.path.join(path, name))
        )
        return ignore

    return _ignore_patterns


def removeEmptyFolders(path):

    # https://metinsaylan.com/7974/gist-python-script-to-delete-all-empty-folders/

    for root, dirs, files in os.walk(path, topdown=False):
        for name in dirs:
            try:
                if (
                    len(os.listdir(os.path.join(root, name))) == 0
                ):  # check whether the directory is empty
                    try:
                        os.rmdir(os.path.join(root, name))
                    except:
                        print("FAILED :", os.path.join(root, name))
                        pass
            except:
                pass

    return
