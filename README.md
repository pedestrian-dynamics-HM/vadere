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
* **query** is an user request for a quantity of interest for the specific VADERE setting with the given the scenario 
set in the environment. A query can simulate VADERE for multiple scenario settings for the parameter variation 
(such as a full grid sampling).

#### Getting started

1. Download and install:

**It is highly recommended to install git-lfs to also download the VADERE models.** 

Download source code, pre-set VADERE models and example environments with git:
```
git@gitlab.lrz.de:vadere/suq-controller.git
```

Install package `suqc` by running the following command inside the downloaded folder `suq-contoller`. It is recommended 
to use `python>=3.6`.

```
python3 setup.py install
``` 

Note: In Linux this may have to be executed with `sudo`.

This installs the essential package, but does **not** copy the models or example environments. To set up the 
corresponding folders and copy the models to the appropriate position run the following command separately.

```
python3 setup_folders.py TODO
```

Test if the installed package was installed successfully by running:

```
python3 -c "import suqc; print(suqc.__version__)"
```

This command should print the installed version number in the terminal. In case an error was thrown the package is 
not installed successfully. 


2. Configuration of `suqc`

During the run of `setup_folders.py` two folders are created at the user's home path. 

   1. `.suqc` consists of the meta configuration of the suq-controller. This includes the path to the 
   environment-container folder and information about the available models. 
   2. `suqc_envs` is a container and consists of environments that are set up. 


#### Introduction

See [SRC_PATH]/tutorial
