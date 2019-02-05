# Changelog: Vadere

**Note:** Before writing into this file, read the guidelines in [Writing Changelog Entries.md](Documentation/contributing/Writing Changelog Entries.md).

## In Progress: v0.7

### Added

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
  [table](Documentation/changelLogImages/TopographyCheckerMessages.md) for
  supported warnings and erros as well as this
  [picture](Documentation/changelLogImages/TopographyChecker.png) to see which
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

- Removed directory `Documentation/version-control` which contained the Git hooks. The Git hooks are not required anymore. Instead, added `git rev-parse HEAD` to file `VadereSimulator/pom.xml` to create `VadereSimulator/resources/current_commit_hash.txt` during each build via `mvn compile`.
  **Note:** The file `current_commit_hash.txt` is created during Maven's validation phase, i.e., before the actual build.
`


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
