# Contributing

It is highly recommended to contribute (report bugs, code, ideas, etc.) in the original repository. If you want to update the suq controller with the current master run the following git command from Vadere's root source code path:

```
git subtree pull --prefix Tools/SUQController git@gitlab.lrz.de:vadere/suq-controller.git master --squash
```


### WORK IN PROGRESS

The suq-controller connects the modules "Surrogate Model" (S) and "Uncertainty
 Quantification" (UQ) (see other Vadere group repos). 
The main functionality of the `suq-controller` is to sample parameters from Vadere
 and return the result of specified quantity of interests (QoI) in a convenient format
  ([pandas DataFrame](https://pandas.pydata.org/pandas-docs/stable/generated/pandas.DataFrame.html)). 

## Getting started

Either install as a Python package or run the source code directly. Either way it is recommended to use Python>=3.6 


#### Use source directly:

Run the code from the source directly (without install), please check if you meet the requirements (see `requirements.txt` file). You can also run `pip3 install -r /path/to/requirements.txt`. Make also sure that the Python paths are set correctly (possibly add with `sys`). 

#### Install as Python package:

To install as a package `suqc` run 
```
python setup.py install
``` 

from the command line. (Note: In Linux this may have to be executed with `sudo`).

Test if the installed package was installed successfully by running:

```
python -c "import suqc; print(suqc.__version__)"
```

This command should print the installed version number (and potentially infos to set up required folder) in the terminal. In case an error was thrown the package is 
not installed successfully. 

### Introduction

See [SRC_PATH]/tutorial
