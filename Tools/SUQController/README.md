# Contributing

It is highly recommended to contribute (report bugs, code, ideas, etc.) in the original repository. If you want to update the suq controller with the current master run the following git command from Vadere's root source code path:

```
git subtree pull --prefix Tools/SUQController git@gitlab.lrz.de:vadere/suq-controller.git master --squash
```


### WORK IN PROGRESS

The suq-controller connects the modules "Surrogate Model" (S) and "Uncertainty Quantification" (UQ) (see other vadere Repos). 
The main functionality of the `suq-controller` is to query many differently parametrized VADERE scenarios and 
return the result of specified quantity of interests (QoI) in a convenient format ([pandas DataFrame](https://pandas.pydata.org/pandas-docs/stable/generated/pandas.DataFrame.html)). 


This git repository uses git large file storage (git-lfs). This allows to ship default VADERE models (larger .jar files.)
with the git repository. 

For developers: To install git-lfs follow the instructions [here](https://github.com/git-lfs/git-lfs/wiki/Installation)
In file `.gitattributes` in the repo shows the settings for git-lfs. 


### Glossary

Other words were used in this project to not confuse terminology with VADERE (such as `scenario` and `project`). 

* **container** is the parent folder of (multiple) environments
* **environment** is folder consisting of a specifed VADERE scenario that is intended to query
* **request** is an user request for a quantity of interest for the specific VADERE setting with the given the scenario 
set in the environment. A query can simulate VADERE for multiple scenario settings for the parameter variation 
(such as a full grid sampling).

## Getting started

Either install as a Python package or run the source code directly. Either way it is recommended to use Python>=3.6 

### Using the code



#### Use source directly:

Run the code from the source directly (without install), please check if you meet the requirements (see `requirements.txt` file). You can also run `pip3 install -r /path/to/requirements.txt`. Make also sure that the Python paths are set correctly (possibly add with `sys`). 

#### Install as Python package:

To install as a package `suqc` run 
```
python3 setup.py install
``` 

from the command line. (Note: In Linux this may have to be executed with `sudo`).

Test if the installed package was installed successfully by running:

```
python3 -c "import suqc; print(suqc.__version__)"
```

This command should print the installed version number (and potentially infos to set up required folder) in the terminal. In case an error was thrown the package is 
not installed successfully. 

### Introduction

See [SRC_PATH]/tutorial
