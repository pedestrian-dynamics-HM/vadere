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


#### Using SUQC and Vadere 
Here a few hints for your .scenario file for Vadere:

1.  ScenarioChecker  
    Before running your scenario automatically on suqc, activate the ``ScenarioChecker`` (Project > Activate ScenarioChecker) and run it in the ``VadereGui``.
   The ScenarioChecker will point out potential problems with your scenario file. 
2.  Default parameters  
    Make sure to set ``realTimeSimTimeRatio`` to 0.0. (Values > 0.0 slow down the simulation for the visualisation)  
    Another point that may cost a lot of computation time is the ``optimizationType``, consider using ``DISCRETE`` (discrete optimization) instead of ``NELDER_MEAD``. Please note, that ``varyStepDirection`` should always be activated with discrete optimization.  
    Remove ``attributesCar`` from the .scenario file if you are not using any vehicles to avoid confusion of attributes. 
3.  Visual check   
    Visually check the results of your simulation, maybe check upper and lower parameter bounds. 
4.  Clean topography  
    Remove elements in your topography that are not used. Sometimes through the interaction with the mouse, tiny obstacles or targets are created unintentionally. 
    Check the elements in your topography, you can take a look at the ``ElementTree`` in the Topography creator tab. Remove all elements that are unused, especially focusing on targets. 
5.  Data processors  
    Remove all data processors and output files that you don't use. In particular, remove the overlap processors, they are intended for testing purposes. 
6.  Reproducibility  
    Make sure that your runs are reproducible - work with a ``fixedSeed`` by activating ``useFixedSeed`` or save all the ``simulationSeed``s that have been used. 
   (Another way is to provide a ``fixedSeed`` for each runs with suqc, in this case make sure that ``useFixedSeed`` is true.)


