# Changelog: Vadere

**Note:** Before writing into this file, read the guidelines in [Writing Changelog Entries.md](Documentation/contributing/Writing Changelog Entries.md).

## In Progress:

### Added

### Removed

### Changed

### Fixed

### Performance

### Security

### Deprecated

### Other

## v2.4 (2022-10-04)

### Added
#### Gui - AttributeTree
- Added `AttributeTreeModel` which represents an object instances fields.
- Added `FieldNode` which represents a field of a registered type.
- Added `ObjectNode`which represents a field with an object instance of an unregistered type.
- Added `ArrayNode` which represents a field of type java.util.List<E>
- Added `AbstrNode` which represents a field which can be instanced by subclasses of the given type
#### Gui - Attribute Table
- Added `AttributeTableContainer` which delegates calls from `TopographyCreatorModel` to the `AttributeTableView` to remove the dependent call to `getAttributes`.
- Added `AttributTableView` which stores multiple `AttributeTablePage`.
- Added `AttributeTablePage` which hold a reference to a node of the `AttributeTreeModel` as the root node of this page.
- Added `JAtttributeTable` which servers as a layouter for the `AttributeEditor`s by using a `JAttributeTable.Styler`.
- Added `JAttributeTable.Styler` which is used to build a view delegate for each row of the table.
- Added `AttributeEditor` representing the abstract superclass for implementing custom editors.
- Added `SpinnerCellEditor` for editing `Interger` instance.
- Added `DoubleSpinnerCellEditor` for editing `Doubles` instance.
- Added `CheckBoxCellEditor` for editing `Boolean` instance.
- Added `ComboBoxCellEditor` for selecting enum types.
- Added `TextEditCellEditor` for editing `String` instance.
- Added `VPointCellEditor` for editing `VPoint` instance.
- Added `VShapeCellEditors` for editing `VShape` subclassed instances.
- Added `ChildObjectCellEditor` which provides an embedded `AttributeTablePage` for the corresponding object instance.
- Added `AbstractTypeCellEditor` which provides an embedded `AttributeTableView` containing pages for each subclass of an abstract type.
- Added `ListCellEditor` which provides an embedded `JAttributeTable` to place editors in a list.
- Added `AttributeHelpView` which displays javadoc help for a selected field.

#### Gui
- Introduced FlatLightLaf theme as default theme.
- Added a common icon theme for icons in the topography creator.
- 
#### State
Spawners were introduced to encapsulate logic and data of the distributions that were prior to this only used for sources
- Added `LerpSpawner`
- Added `MixedSpawner`
- Added `RegularSpawner`
- Added `TimeSeriesSpawner`
### Removed
- Targets are not able to represent traffic lights anymore. Removed corresponding attributes.
### Changed
- Attribute based classes now inherit commonly used getters and setters for common attribute fields.
- ScenarioElement like classes now inherit from AttributeAttached
- Introduced spawners into sources to abstract the use of distributions for spawning agents.
- Encapsulated absorbing and waiting behaviours into their own attribute classes.
- VDistributions now only provide the `getNextSample(..)` method other methods were moved into the corresponding spawner classes.
- Moved and renamed some attributes from distribution classes to spawner classes
- In Gui/TopographyCreator, corrected create method for sources from Pedestrian to Circle

## v2.3 (2022-09-01)

### Added
- Added new parameter `waitingTimeDistribution` to `AttributesTarget` describing a distribution used for the agents waiting time at a target.
- Added new parameter `distributionParameters` to `AttributesTarget` describing the parameters of `waitingTimeDistribution`.
- Added new parameter `waitingMode` to `AttributesTarget` used for differentiating between individual waiting and traffic light waiting. 
  - can be set to `noWaiting` if agents should be absorbed immediately
  - can be set to `individual` if agents should wait with individually assigned waiting times
  - can be set to `trafficLight` if the target represents a traffic light
### Removed
- Removed parameter `waitingTime` from `AttributesTarget`, because this is now described by `distributionParameters`. 
### Changed

### Fixed

### Performance

### Security

### Deprecated

### Other

## v2.2 (2022-05-24)

### Changed
- Change stimulus behavior

## v2.1 (2022-03-24)

### Added

- Added new output processor "PedestrianTargetReachTimeProcessor" to log the time when an agent reaches its target.
- Added settings in `SettingsDialog` that allow to define opacity of aerosol clouds and agent coloring depending on their degree of exposure (`GUI`)
- Added `AbstractExposureModel` describing the exposure of healthy agents to infectious agents (`Simulator`)
* `AirTransmissionModel` as a new version of the previous `TransmisisonModel` (see section `changed`)
* `ProximityExposureModel`: Agents become exposed depending on the distance to infectious agents.
- Added `AbstractDoseResponseModel` describing the probability of infection depending on a pedestrian's individual exposure. (`Simulator`)
- `Pedestrian`: added new properties (`State`)
* Added attribute healthStatus of type T extends `ExposureModelHealthStatus` (corresponding to the new exposure model)
* Added attribute infectionStatus of type T extends `DoseResponseModelInfectionStatus` (corresponding to the new dose response model)

### Changed

- In `postLoop()` of `Simulation.java`, clear the topography as very last step so that models and output processors can use it before.
- Refactoring of the software architecture of `TransmissionModel` (`Simulator`, `GUI`, `State`):
* Renamed `TransmissionModel` to `AirTransmissionModel` to avoid confusion with transmission in the sense of communication or others.
* The previous concept of the TransmissionModel is now divided into exposure (`AbstractExposureModel`) and dose response (`AbstractDoseResponseModel`). The abstract classes allow for model enhancements or implementing alternative models.
* Adapted the model attributes so that they fit to the new exposure models and dose response models.
* Minor improvements / simplifications of the aerosol cloud model; Reduced complexity of the transmission model; Now, the model creates only circular but no elliptical clouds;
* Moved some hidden parameters to scenario file to make the model more flexible
* Move attributes that are equal for all instances of classes AerosolCloud, Droplets, and Pedestrian to the general model attributes (`AttributesAirTransmissionModel`). This is not strictly object-oriented but pragmatic and avoids redundancy.
- `TikzGenerator`: Make the transparency of aerosol clouds in the tex file depend on the pathogen concentration. (`GUI`)
- `SettingsDialog`: 
* increase contrast between color of settings wheel and vadere GUI background color (2d2edad5: `GUI`)
* reduce width of color panels (`GUI`)
- Renamed

### Fixed
- `TikzGenerator`: Now, the tex file (generated with tikz snapshot during onlinevisualization) also contains aerosol clouds (29e9c937: `GUI`)
- `SettingsDialog`: Fixed overlapping elements in postvis settings dialog. (`GUI`)
- `TopographyController`: Previously, pedestrians that were directly put into the topography (not spawned by sources) could not be accessed / manipulated by the TransmissionModel, that is one could not define a health status. These pedestrians are now handled by the TopographyController. (`Simulator`)

## Removed
- Due to refactored architecture of the transmission model, removed...
* classes `HealthStatus`, `NumberOfPedPerInfectionStatusProcessor`, `PedestrianHealthStatusProcessor` (`State`, `Simulator`)
* enum `InfectionStatus` (`State`)


## v2.0 (2021-12-14)

### Changed
- `SourceController`: Spawn agents according to time series. The number of agents was previously defined by a distribution function during spawning. In addition, there is now the possibility to store a time series. For this purpose, the definition of the sources was generalised.


## v1.16
### Added
- `CognitionModel`: Make cognition model probabilistic. In the `GUI`, the user can define `ReactionProbabilites` that assign a probability to each stimulus using a unique id.


## v1.15 (2020-11-12)

### Added

- The TikZ snapshot button allows to export either the whole topography or only the current (scaled) view. This is useful when visualizing parts of bigger topographies.
- Added "--version" to "vadere-console.jar".
- Added a Vadere logo (a simple "V") as window and task bar icon to get rid of the lame default Java icon.
- Added "Psychology" tab in GUI which shows all psychology-related attributes of a scenario (which are stored in JSON node "attributesPsychology").
- Add `PedestrianPotentialProcessor` which writes out different potentials (TARGET (target potential), OBSTACLE (obstacle potential), PEDESTRIAN (agent potential), ALL (sum of all)) configureable via its `Attributes`. It only writes those potentials if the used main model for the simulation is a `PotentialFieldModel`.
- Add `AttributesPedestrianPositionProcessor` to the `PedestrianPositionProcessor` such that the user can disable interpolation (using the default it is enabled), making it more flexible.
- Positions can now be interpolated i.e. Agents offer two methods: `getPosition` and `getInterpolatedPosition`. `getPosition` returns the Position the model is working with and `getInterpolatedPosition` the position the agent is approximatily at. For ODE-based models both methods return the same position but for footstep based models like the OSM the position is interpolated assuming that an agent performs his current step with a constant speed. This gives more accurate positions for visualizing and computing measures like the density.
- Added new scenario element `TargetChanger`. This scenario element has an arbitrary shape and changes the target of an agent. Either to another static target or to another agent (to get a follower behavior). A `TargetChanger` has two important parameters:
  * `changeTargetProbability`: This defines how many percent of the agents, who enter the area, should change their target.
  * `nextTargetIsPedestrian`: If `nextTargetIsPedestrian == false`, assign a new static target. Otherwise, randomly choose a pedestrian (with given target id) to follow.
- Add Scenario Hash to info panel below of the TopographyCreator and Post-Visualisation View:
  * will show if changes to the scenario will change the floorfield 
  * Hover over hash to see full value
  * Left-Click to copy full hash to clipboard.
- TraCI commands: 
  * getHash: return scenario hash for given scenario 
  * CompoundObject implementation to allow complex get/set commands (i.e. create pedestrian hat 
    random location during simulation run)
- osm2vadere converter:
  * it is possible to specify a way with the tag `area-of-intrest` (AOI). If this is present and the 
    corresponding command line argument is given, only elements within the bounding box of the
    AOI will be converted.
- Add INET environment export:
  * create an INET environment xml file based on the current topography. For now only prism shapes 
    are possible with a fixed height of 5m.
- vadere-console `utils` subCommand:
  * A miscellaneous collector of simple function operating on a single scenario file.
  * -f (input file or directory depending on method m [required]) 
  * -o (ouput file or directory depending on method m [optional]) 
  * -m (name of method.) See subparser help for allowed functions.
  * currently implementd:
    * `getHash`: Hash value of given scenario. (-o option ignored)
    * `binCache`: calculate binary cache (-o must be a directory. If missing it will be created)
    * `txtCache`: calculate text based cache (-o must be a directory. If missing it will be created)


### Changed

- Refactored EikMesh code.
- Refactored psychology-related code and added several test scenarios.
- Refactored TikZ generator code.
- The behavioral heuristics model (BHM) can now be used with a floor field. I.e., BHM agents are able to derive the geodetic distance to their targets instead of using the Euclidean distance.
- The "Time" slider in the PostVis uses the resolution of the "Res." slider instead of using a fixed step size of 0.4 seconds.
- The Model (of the GUI MVC) of the Postvisualization changed to a DataFrame based structure using [Tablesaw](https://github.com/jtablesaw/tablesaw) which is based on [FastUtils](http://fastutil.di.unimi.it/).
- `FootStepProcessor` interpolates the pedestrian's foot step to obtain a more precise position.  
Was previously known as `PedestrianFootStepProcessor`
- Use following shortcuts for zooming and scrolling in the topography creator:
  * Ctrl + Mouse Wheel Scroll: Zoom in/out.
  * Mouse Wheel Scroll: Scroll vertically.
  * Shift + Mouse Wheel Scroll: Scroll horizontally.
  * Use Alt key to decrease the step size while scrolling.
- CachePath lookup:
  The new cache lookup now allows a 'global' lookup. Previously all cache files are saved in 
  a `__cache__` folder relative (as sibling) to the currently running scenario file. This works 
  good for local testing and runs. However, if one scenario is duplicated and integrated in other 
  projects the same cache would be created at multiple locations. The current solution would be 
  to enter an absolute path as the `cacheDir` but this will break interoperatbility between 
  windows and linux as well as sharing scenario files with other users. CacheDir Lookup order:
  1. `cacheDir` is an absolute path: Use it; and log the path to console.
  2. `cacheDir` is relative and  `Vadere.cache.useGlobalCacheBaseDir=false` (default): 
     save cache in a `__cache__` folder relative (as sibling) to the currently running scenario file
  3. `cacheDir` is relative and  `Vadere.cache.useGlobalCacheBaseDir=true`:
     Lookup `Vadere.cache.flobalCacheBaseDir` and use this as the base path for the relative 
     `cacheDir` path. `Vadere.cache.flobalCacheBaseDir` defaults to `${user.home}\.cache\vadere`

## v1.4 (2019-09-05)

### Changed 

- Renamed "footStepsToStore" to "footstepHistorySize" in all scenario files.

## v1.3 (2019-07-31)

### Added 

- new `referenceCoordinateSystem` attributes in the topography element (default: `null`). This 
  object holds information about the base coordinate system and an optional translation used 
  on the coordinates in the topography. If the description field is a free text field which 
  can be used for miscellaneous information. (see osm2vadere converter)
```
"referenceCoordinateSystem" : {
      "epsgCode" : "UTM Zone 32U",
      "description" : "OpenStreetMap export osm2vadere.py-ed63d4e94898a15a6bf25fa59c05a5b2f73d8f74-dirty",
      "translation" : {
        "x" : 692152.0894735109,
        "y" : 5337384.6661008
      }
    }
```

### Changed 

- osm2vadere.py refactored. 
  - Extract osm-xml manipulation into `osm_helper.py`
  - Changes command line structure
- osm_helper.py: cl-based manipulation of osm xml to add additional information into the xml 
  structure readable by osm2vadere.py converter.

## v1.2 (2019-07-13)

### Added

- VadereServer:
  - Introducing TraCI server implementation for Vadere to allow remote control
    of Vaderes simulation loop.
  - VadereManager/target/vadere-server.jar will open a TCP socket and waits
    for connection request.
- FloorField Caching:
  - CellGrid based floorfields can be loaded from a persisted cache file.
  - Added attributes:
    - `cacheType: [NO_CACHE|TXT_CACHE|BIN_CACHE]`
    - `cacheDir: ""` relative path
  - Cache files will be saved in a `__cache__` directory beside (sibling) the
    scenario file. With `cacheDir` it is possible to create some structure within
    the cache directory. Important: If `cacheDir` is an absolute path the cache
    file will not be placed in `__cache__`.
  - A TXT_CACHE type will save the CellGrid in a human readable form (CSV) and
    BIN_CACHE will use a simple binary format for better performance and space
    reasons.
- TikzGenerator:
  - add configuration to show all traces of pedestrians, even if they
    left the simulation. Config: PostVis -> `Show all Trajectories on Snapshot`
  - add named coordinate for each scenario element. The coordinate represents the
    centroid point of the polygon and can be used as a point for relative placement
    of labels.
  - introduced id for reference: Source `src_XXX`, Target `trg_XXX`, AbsorbingArea `absorb_XXX`
    Obstacels `obs_XXX`, Stairs `str_XXX`, MeasurementArea `mrmtA_XXX`
- single step mode in GUI: Allows the user to step through the simulation one
  step at a time to identify bugs.
- simplify obstacles (with undo support): Merge multiple obstacles based on the
  convex hull their points create. The merge can be undon
- add features to open street map (osm) importer:
  - import 'open' paths as polygons with a specified width. With this it is
    possible to create walls or subway entrance
  - add option to include osm ids into each obstacle created

`PostVis` added functionalities:
- the PostVis (optionally) works now on the basis of simulation time instead of time steps. Agents' position will be interpolated. The option can be enabled and disabled via the `SettingsDialog`
    - the user can jump to a specific simulation time
    - the user can step forward by steps as small as 0.01s
    - the user can make videos using also this new feature which results in very smooth movement
    - the frames per seconds (FPS) is now more accurate

`OnlineVis` added functionalities:
- the OnlineVis (optionally) works now on the basis of simulation time instead of time steps. Agents' position will be interpolated. The option can be enabled and disabled via the `SettingsDialog`.

### Changed

- version migration ensures that new attributes in the scenario file will be added
  with default values if no specific actions are defined in a Transformation class.
- TikzGenerator: style information for pedestrians are moved to dedicated style
  classes to simplify changes in generated tikz files.  

## v1.0 (2019-06-13)

### Added

- Open a trajectory file in the stand-alone application `vadere-postvis.jar` via drag and drop from file explorer.
- Scenario elements like obstacles and targets can now be resized using the mouse in the topography creator tab (first, select element via left-click and then move mouse to one of its edges to get the resize handles).
- Draw also Voronoi diagram in `TikzGenerator`.
- Added new scenario element `AbsorbingArea` to absorb agents immediately.
  * The new scenario element can be selected in `TopographyCreator` via the "emergency exit" button.
  * The new scenario element is also taken into account by `SettingsDialaog` and `TikzGenerator`.
- Configured 1-click-deployment in ".gitlab-ci.yml".
  * In short: Vadere is packaged as a single ZIP file containing following build artifacts: README.md Scenarios/ VadereGui/target/vadere-gui.jar VadereSimulator/target/vadere-console.jar
  * Vadere is packaged for GNU/Linux and Microsoft Windows.
  * The ZIP file can be accessed on the website via: http://www.vadere.org/releases/
  * The website's filesystem contains three subdirectories for releases:
    - builds/master: For releases on "master" branch (automatically triggered when pushing to "master" branch).
    - builds/stable: For Git tags on on any branch (automatically triggered when pushing tags).
    - builds/branches: For releases of any other branch (manually triggered in web interface: https://gitlab.lrz.de/vadere/vadere/pipelines)
  * The link to the deployed items can be also found in the GitLab web interface: https://gitlab.lrz.de/vadere/vadere/environments
- Create a Facade for the logging in vadere. This will remove 3rd party
  dependencies in source most files. Only a small set of facade classes
  will have the dependency.
- Assertion output to StdErr is collected by a dedicated logger see `StdOutErrLog.java`.
  Each entrypoint must call `StdOutErrLog.addStdOutErrToLog()` to activate this
  Logging.
- Logging API did *not* change. Each log level has an additional String formater
  method like `log.errorf("Erron in %s with value of x: %d","ClassA", 42)`
- issues closed: [issue#163](https://gitlab.lrz.de/vadere/vadere/issues/163)
- Added script "osm2vadere.py" to "Tools" folder, to convert OpenStreetMap maps to a Vadere topography. (929d4775: Tools).
- `VadereConsole`: Add option `--logname <filename>` to specify the name for the log file.
  Please note that the log file `log.out` is always written (because this file is configured
  in the `log4j.properties` of each Vadere module (i.e., "gui", "meshing", ...). (c61a3946: Simulator)
- New outputprocessors
  * mainly for the BHM: QueueWidthProcessor (to evaluate queueWidth) and PedestrianBehaviorProcessor (evaluate behavior: step / tangential step / sideways step / wait)
  * solely for the OSM: PedestrianFootStepProcessor (logs every step instead of the positions at each time step )

- `TopographyChecker`: In package org.vadere.simulator.utils, added TopographyChecker to show
  user if the current topography contains problems. The  TopographyChecker
  will check for overlapping `ScenarioElements` and check for inconsistence
  settings such as missing  TargetIDs for sources or inconsistenct speed ranges
  for pedestrians. See this
  [table](Documentation/changelog/TopographyCheckerMessages.md) for
  supported warnings and erros as well as this
  [picture](Documentation/changelog/TopographyChecker.png) to see which
  kind of overlap produces erros or warnings.
- `VadereConsole`: Add `--output-dir [on, off]` (default: `on`) switch to  `scenario-run`
  sub-command. This will will turn the ScenarioChecker on or off for the command
  line. If the Checker detects an error  the simulation will not be executed.

- `TopographyCreator` added functionalities:
    - move all topography elements by some dx, dy [issue#171](https://gitlab.lrz.de/vadere/vadere/issues/171)
    - move the whole topography by some dx, dy [issue#148](https://gitlab.lrz.de/vadere/vadere/issues/148)
    - automatically merge overlapping obstacles by using the weiler algorithm.

- `Processors`:
    - new processors to compute the fundamental diagrams (by using different velocity, flow and density measurement methods)

- `GUI`:
    - improved coloring

- `OutputFile`: Distinguish between indices (rows) and headers (columns), in code and with a new checkbox that when enables allow to write meta-data into output files. Furthermore, resolve naming conflicts (if they occur) in headers of output files (see #201).

### Changed

- Renamed Vadere JAR files.
  * vadere.jar -> vadere-gui.jar
  * postvis.jar -> vadere-postvis.jar
  * vadere-console.jar -> remained unchanged
- Header in output file have now the following form "[NAME]-PID[ID]". This avoids name conflicts and makes mapping to the relevant processor easy and fast.
- Migration to Java 11 (OpenJDK).
- Removed directory `Documentation/version-control` which contained the Git hooks. The Git hooks are not required anymore. Instead, added `git rev-parse HEAD` to file `VadereSimulator/pom.xml` to create `VadereSimulator/resources/current_commit_hash.txt` during each build via `mvn compile`.
  **Note:** The file `current_commit_hash.txt` is created during Maven's validation phase, i.e., before the actual build.

## v0.6 (2018-09-07)

### Added

- Graphical simulation result is displayed in a table view to show run-time and overlap information if the corresponding processors are loaded. The Simulation result dialog can be deactivated in the preferences. (5ffca5a3: Simulator, GUI)
- Added new OutputProcessors for Overlaps. (8028c523: Simulator)
- Added "fixedSeed" and "simulationSeed" to AttributesSimulation. (79268262: Simulator)
- VadereConsole.jar migrate will migrate all scenario files within the specified directory and all child directories. (37fde165: Simulator)
  * To exclude specific sub-trees or only specific directories the igonoreDirs List can be expanded.
  * DO_NOT_MIGRATE or .DO_NOT_MIGRATE: Ignore current directory but continue with existing child directories.
  * DO_NOT_MIGRATE_TREE or .DO_NOT_MIGRATE_TREE: Ignore the directories and the complete sub-tree.
- Added a new OutputProcessor, NumberOverlapsProcessor. This processor saves the number of overlaps that occurred during a simulation run. It needs the PedestrianOverlapProcessor. (57d90e93: Simulator, State)
- Added sub commands to "VadereConsole": project-run, scenario-run, suq, migrate (c7e0538c: GUI)
- In the onlinevisualization it is now possible to display the target potential field of a selected pedestrian. (123457aa: GUI)

### Changed

- PedestrianOverlapProcessor returns two values "distance", "overlaps"for each overlap detected. (5ffca5a3: Simulator)
  * If no overlap occurs the output is empty.
  * "distance": The distance between the center of the two pedestrians
  * "overlaps": The amount the two pedestrian overlap
- Moved and renamed attributes in scenario files. (c7e0538c: State)
  * Move /scenario/attributesModel/org.vadere.state.attributes.models.AttributesCGM/groupSizeDistribution to each source in /scenario/topography/sources/[]/groupSizeDistribution. This allows different group size distribution for each source
  * Rename /scenario/attributesModel/*/timeCostAttributes/standardDeviation to standardDeviation.

### Performance

- Faster distance computation. (6214738c: Simulator)
  * To compute the distance from a point x the closest obstacle we compute the distances on a Cartesian grid of cell size equal to the default value of AttributesFloorField.potentialFieldResolution. There are two methods to compute the distances.
  * Brute force (default): Compute the distance for a grid point x by computing all distances (for all obstacles) taking the minimal
  * Eikonal Equation solvers (unused): Use obstacles to be the targets are of the eikonal equation and solve the equation using one of the solvers (default is the Fast Marching Method).

## v0.2 (2016-12-22)

### Added

- Stability and usability improved, additional pedestrian simulation models are supported. (babf0b67: GUI, Simulator, State, Utils)

## v0.1 (2016-08-05)

### Added

- Initial release of the software as open source. (72391fab: GUI, Simulator, State, Utils)
