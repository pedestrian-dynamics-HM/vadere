**Note:** Before writing into this file, read the guidelines in [Writing Changelog Entries.md](Documentation/contributing/Writing Changelog Entries.md).

# In Progress: v0.7 

## Added
- Two new outputprocessors - mainly for the BHM - to evaluaet queueWidth (QueueWidthProcessor) and behavior (PedestrianBehaviorProcessor)
- In package `org.vadere.simulator.utils`, added `TopographyChecker` to show user if the current topography contains problems. The 
  `TopographyChecker` will check for overlapping `ScenarioElements` and check for inconsistence settings such as missing 
  TargetIDs for sources or inconsistenct speed ranges for pedestrians. See this [table](Documentation/changelLogImages/TopographyCheckerMessages.md) for supported warnings and erros
  as well as this [picture](Documentation/changelLogImages/TopographyChecker.png) to see which kind of overlap produces erros or warnings.

# v0.6 (2018-09-07)

## Added

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

## Changed

- PedestrianOverlapProcessor returns two values "distance", "overlaps"for each overlap detected. (5ffca5a3: Simulator)
  * If no overlap occurs the output is empty.
  * "distance": The distance between the center of the two pedestrians
  * "overlaps": The amount the two pedestrian overlap
- Moved and renamed attributes in scenario files. (c7e0538c: State)
  * Move /scenario/attributesModel/org.vadere.state.attributes.models.AttributesCGM/groupSizeDistribution to each source in /scenario/topography/sources/[]/groupSizeDistribution. This allows different group size distribution for each source
  * Rename /scenario/attributesModel/*/timeCostAttributes/standardDerivation to standardDeviation.

## Performance

- Faster distance computation. (6214738c: Simulator)
  * To compute the distance from a point x the closest obstacle we compute the distances on a Cartesian grid of cell size equal to the default value of AttributesFloorField.potentialFieldResolution. There are two methods to compute the distances.
  * Brute force (default): Compute the distance for a grid point x by computing all distances (for all obstacles) taking the minimal
  * Eikonal Equation solvers (unused): Use obstacles to be the targets are of the eikonal equation and solve the equation using one of the solvers (default is the Fast Marching Method).

# v0.2 (2016-12-22)

## Added

- Stability and usability improved, additional pedestrian simulation models are supported. (babf0b67: GUI, Simulator, State, Utils)

# v0.1 (2016-08-05)

## Added

- Initial release of the software as open source. (72391fab: GUI, Simulator, State, Utils)
