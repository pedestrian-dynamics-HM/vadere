# Changelog: Vadere

**Note:** Before writing into this file, read the guidelines in [Writing Changelog Entries.md](Documentation/contributing/Writing Changelog Entries.md).

## In Progress:

### Added

### Changed

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
- the PostVis works now on the basis of simulation time instead of time steps. Agents' position will be interpolated.
    - the user can jump to a specific simulation time
    - the user can step forward by steps as small as 0.01s
    - the user can make videos using also this new feature which results in very smooth movement
    - the frames per seconds (FPS) is now more accurate

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
  * In short: Vadere is packaged as a single ZIP file containing following build artifacts: README.md VadereModelTests/ VadereGui/target/vadere-gui.jar VadereSimulator/target/vadere-console.jar
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
