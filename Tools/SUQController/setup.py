#!/usr/bin/env python3

# TODO: """ << INCLUDE DOCSTRING (one-line or multi-line) >> """

import os

from setuptools import setup, find_packages

from suqc.configuration import SuqcConfig
from suqc import __version__

# --------------------------------------------------
# people who contributed code
__authors__ = "Daniel Lehmberg"
# people who made suggestions or reported bugs but didn't contribute code
__credits__ = ["n/a"]
# --------------------------------------------------

# To generate a new reqirements.txt file run in console (install vis 'pip3 install pipreqs'):
# pipreqs --use-local --force /home/daniel/REPOS/suq-controller

with open('requirements.txt', "r") as f:
    requirements = f.read().splitlines()

# Writes a file that gives information about the version such that "suqc.__version__" provides the current version,
# which is a convention in Python:
with open(SuqcConfig.path_package_indicator_file(), "w") as file:
    file.write(f"version={__version__}")

assert os.path.exists(SuqcConfig.path_package_indicator_file())

setup(
    name="suqc",
    version=__version__,
    license="LGPL",
    url="www.vadere.org",
    packages=find_packages(),
    install_requires=requirements,
    data_files=[('suqc', ["suqc/PACKAGE.txt"])]
)

os.remove(SuqcConfig.path_package_indicator_file())
assert not os.path.exists(SuqcConfig.path_package_indicator_file())