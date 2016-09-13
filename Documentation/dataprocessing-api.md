# Data processing API

## Using

Every data is computed by a __processor__. A dataProcessor has to have a certain __key__ (K) and a certain __value__ (V). Only dataProcessors with the same key can be put together into an __output file__. The dataProcessor's key indicates the type of data computed.

There are already some predefined key classes:

	1. TimestepDataKey
	2. PedestrianIdDataKey
	3. TimestepPedestrianIdDataKey
	(4. NoDataKey)

__Example__: The class _PedestrianPositionProcessor_ is responsible for "computing" and storing the positions of each pedestrian at every point in time meaning that the corresponding key type is the _TimestepPedestrianIdDataKey_. So, the PedestrianPositionProcessor can be combined with other dataProcessors whose data is identified by the same key type (e.g. _PedestrianVelocityProcessor_). The value type is _VPoint_.

__Example__: The class _PedestrianEvacuationTimeProcessor_ computes the evacuation time for every pedestrian. A certain evacuation time can be associated with a pedestrian. So, the key type in this case is the _PedestrianIdDataKey_. The value type is _double_.

The key type _NoDataKey_ is constructed for scenario wide data with no specific identification needed (e.g. (mean) evacutation time).

Every output file can only store dataProcessor data of exactly one key type, i.e. there is for example no possibility to store data from a PedestrianPositionProcessor and a PedestrianEvacuationTimeProcessor in one file.

For each key, there exists a corresponding output file type:

	1. TimestepOutputFile
	2. PedestrianIdOutputFile
	3. TimestepPedestrianIdOutputFile
	(4. NoDataKeyOutputFile)



The data processing's JSON structure is:
```json
{
	"files": [ {
    	"type": (output file type),
        "filename": (filename),
        "dataProcessors": [ (dataProcessor ids) ],
        "separator": (separator's sign in output file, optional, default is space)
    }, ... ],
    "dataProcessors": [ {
    	"type": (dataProcessor type),
        "id": (dataProcessor's id),
        "attributesType": (attributes' type, optional),
        "attributes": {
        	(attribute name): (attribute name),
            ...
        } (optional)
    } ]
}
```

An example for a short data processing definition is:
```json
{
  "files": [ {
    "type": "org.vadere.simulator.projects.dataprocessing.outputfile.TimestepPedestrianIdOutputFile",
    "filename": "output_ts_pid.txt",
    "dataProcessors": [ 1, 2, 3 ],
    "separator": ";"
  } ],
  "dataProcessors": [ {
    "type": "org.vadere.simulator.projects.dataprocessing.dataProcessor.PedestrianPositionProcessor",
    "id": 1
  }, {
    "type": "org.vadere.simulator.projects.dataprocessing.dataProcessor.PedestrianVelocityProcessor",
    "id": 2,
    "attributesType": "org.vadere.state.attributes.dataProcessor.AttributesPedestrianVelocityProcessor",
    "attributes": {
      "pedestrianPositionProcessorId": 1
    }
  }, {
    "type": "org.vadere.simulator.projects.dataprocessing.dataProcessor.PedestrianDensityCountingProcessor",
    "id": 3,
    "attributesType": "org.vadere.state.attributes.dataProcessor.AttributesPedestrianDensityCountingProcessor",
    "attributes": {
      "pedestrianPositionProcessorId": 1,
      "radius": 1.5
    }
  } ]
}
```

As you can see in this example, there is a possibility to use a dataProcessor's data as source for a new computation. That means that data does not have to be computed multiple times. Here, the velocity dataProcessor uses positions from the positions dataProcessor to calculate the velocity.

The output file is saved in the projects' output' folder of the scenario run (with a timestamp), e.g. in _output/RiMEA-4_2016-09-12_12-36-40.341_.

## Enhancing

There are two important base classes:

	1. OutputFile<K>
	2. Processor<K,V>

Common functionality for every output file, and every dataProcessor respectively, is located in these abstract classes.

### Creating a new dataProcessor

If you want to create a new dataProcessor, you have to inherit from the _Processor_ base class and simultaneously specify the key (K) and value (V) type. Let us say, you want to count the amount of pedestrians currently located in the scenario. Since the amount of pedestrian is only timestep dependent, you have to choose the _TimestepDataKey_ as the suitable key type. The value type in this case is _int_. The class definition would look something like:

```java
class AmountPedestriansProcessor extends Processor<TimestepDataKey, Integer> { ... }
```

You can override the significant methods from the base class:
```java
public void preLoop(SimulationState state) { }
protected abstract void doUpdate(SimulationState state);
public void postLoop(SimulationState state) { }
public abstract void init(ProcessorManager manager);
```

The _preLoop_ and _postLoop_ methods are called just before, respectively after, the simulation loop starts, respectively ends.

The most important method is _doUpdate_. It is called in every timestep with the current simulation state as argument. In this method, you have the possibility to update your data. Coming back to our example, an implementation of _doUpdate_ can look like:

```java
protected abstract void doUpdate(final SimulationState state) {
	int timeStep = state.getStep();
    int amountPeds = state.getTopography().getElements(Pedestrian.class).size();
    this.addValue(new TimestepDataKey(timeStep), amountPeds);
}
```

The _init_ method is called when the scenario is loading its dataProcessors. It can be used for getting necessary attributes/parameters for the dataProcessor's computation. The corresponding attributes, defined in JSON, can be accessed via the _getAttributes_ method of the base class. In the case of our example, there is no initialization needed, which results in an empty implementation. But in case of the velocity dataProcessor, a possible implementation can look like:

```java
class PedestrianVelocityProcessor extends Processor<TimestepPedestrianIdDataKey, Double> {
	private PedestrianPositionProcessor pedPosProc;
	// other stuff
    
    @Override
    public void init(ProcessorManager manager) {
    	
    }
}
```
