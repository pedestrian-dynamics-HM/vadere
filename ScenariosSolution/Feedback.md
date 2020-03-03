# Feedback for "Scenarios/README.md"

## Simon Rahn (2020-03-03)

- The fourth steps under "First Steps", which describes opening a Vadere project file, should be highlighted.
- Describe "Voronoi" button in toolbar: User must draw a rectangle to see a Voronoi diagram.
- `GUI -> Data output`: Mention required "Data keys" for an output file explicitly in README. Otherwise, users are lost. Density processors uses `TimestepPedestrianIdKey`. `EvactuationTimeProcessor` requires `PedestrianIdKey`.
- First, describe data processor. Then, describe file.
- Add a "Tip" where Vadere stores the output.
- `DensityCountingProcessors` requires setting the `radius`.
- Vadere does not offer a split-screen to compare multiple simulation runs (Workaround: Use multiple Vadere instances).
- OpenCL warning pops up at the beginnen (Reason: OpenCL not installed and, hence, no parallelization possible)
- Required time (with interaction with Benedikt Kleinmeier):
  * 1st example: 8 minutes
  * 2nd example: 12 minutes
  * 3rd example: 15 minutes
