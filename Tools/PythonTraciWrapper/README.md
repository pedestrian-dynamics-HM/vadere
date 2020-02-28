# PythonTraciWrapper

This code is a python wrapper around the Java TraCI-implementation in vadere. 

## Work in Progress

If you want to update the python traci wrapper with the current master run the following git command from Vadere's root source code path:


```
git pull -s subtree -Xsubtree=Tools/PythonTraciWrapper PythonTraciWrapper master --squash
```

## Dependencies

python>=3.6

IPython

py4j 

## Run this code

0. Install dependencies 
    - install python
    - install IPython via pip or conda
    - install py4j via pip (see https://www.py4j.org/install.html) 
    
1. cd into python-traci-wrapper

2. In a python environment, run `python setup.py install`

3. Verify installation
    - cd into vadere's root directory
    - checkout branch traci_phsc
    - mvn clean && mvn package -DskipTests
    - Open a python console and run
        ```
        from pythontraciwrapper.py4j_client import Py4jClient
        args = ["--loglevel", "OFF", 
                "--base-path", "Scenarios/Demos/roVer/scenarios",
                "--gui-mode", "True"]
        client = Py4jClient("", args)
        client.startScenario("scenario002")
        ```

## Run multiple clients in parallel

To run multiple clients, three port numbers have to be specified for each client without collisions:
- the manager port: where the VadereManager runs
- the java port: the port of the py4j EntryPoint
- the python port: here the python callback server listens

For two clients, this means six different port numbers are required.

```
from IPython import embed
from pythontraciwrapper.py4j_client import Py4jClient
baseArgs = ["--loglevel", "OFF", 
            "--base-path", "Scenarios/Demos/roVer/scenarios",
            "--gui-mode", "True"]
args1 = baseArgs + ["--port", "9997", "--java-port", "10001", "--python-port", "10002"]
args2 = baseArgs + ["--port", "9998", "--java-port", "10003", "--python-port", "10004"]
cli1 = Py4jClient("", args1)
cli2 = Py4jClient("", args2)
scenarioPath = "scenario002"
cli1.startScenario(scenarioPath)
cli2.startScenario(scenarioPath)
embed()
```

```python
if __name__ == "__main__":
    c = Py4jClient.create(
        project_path="VADERE_PATH/Scenarios/Demos/roVer/scenarios",
        vadere_base_path="VADERE_PATH",
        start_server=False,
        debug=False,
    )
    c.entrypoint_jar = "/VADERE_PATH/Tools/PythonTraciWrapper/vadere-traci-entrypoint.jar"
    c.connect()
    c.startScenario("roVerTest002.scenario")
```

## Extend API

If new TraCI commands are implemented in vadere, these can also be used via a python client. To achieve this the following steps are required:

1. Adapt TraciEntryPoint: The TraciEntryPoint class is a server which establishes and handles a connection to vadere via the Manager class. It holds API objects. The implementation of a new API is out of the scope of this readme. Py4j makes it possible for the python callback server to access the API object, which the TraciEntryPoint is holding.
2. Adapt api_wrapper: The PythonTraciWrapper has a python class for each API object of the TraciEntrypoint which is required to be accessed via python. These classes are all inherit from the ApiWrapper class, which implements the object initialisation, including the import of Java classes which have to be accessed via python. If a new TraCI command needs a Java class which is not imported here already, ApiWrapper's __init__ method has to be adapted. In the _start_gateway method of the Py4jClient class, the a new API needs to be bound to instances.
3. Implement wrapper for the new command

In the corresponding api_wrapper, implement a new method. The implementation consists of three parts.
    
1. Convert the input to a compatible type. E.g. a VPoint object is required, use self._vpointClass(x, y)
2. Call the api method on the API object with the compatible parameters.
3. Convert the response such that it satifies the needs of the client.