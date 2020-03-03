# SCCS Chair Retreat 2020 - Vadere Hands-On Session

## Intro

**Vadere** is a simulation framework for pedestrian flow analysis. Vadere's core features:

- Command-line interface (CLI) and an easy-to-use GUI interface.
- Shipped with different locomotion models:
  * Gradient Navigation Model (GNM)
  * Optimal Steps Model (OSM)
  * ...

Motivation for pedestrian stream simulations:

- Improve design of buildings to minimize evacuation times.
- Simulate dangerous situations **without** harming real humans.
- ...

## Goals for Hands-On Session

Get familiar with Vadere - its features and its limitions.

## First Steps

1. Download Vadere from http://www.vadere.org/releases/ (`vadere.retreat2020.<operating_system>.zip`)
2. Unzip `vadere.retreat2020.<operating_system>.zip`
3. Open `vadere-gui.jar`
4. In GUI, open Vadere project containg four examples: `Project -> Open... -> ./Scenarios/Retreat2020/vadere.project`

## Examples

We prepared four small examples:

1. Get started in the first example and compare two different locomotion models in Vadere.
   * **Used scenarios:**
     - 01-MinimalExample-OptimalStepsModel
     - 01-MinimalExample-GradientNavigationModel
   * **Steps:**
     1. Get familiar with the GUI.
     2. Compare different locomotion models by running both simlations.
     3. Compare both simulations in the PosVis (see `Output files` section in the GUI)
        - How do the trajectories differ between OSM and GNM?
        - Draw a Voronoi diagram using the toolbar and compare densities in both runs.
2. In the second example, you should measure the density.
   * **Used scenario:** 02-Density-Measurement
   * **Steps:**
     1. GUI: `Data output`.
     2. Add a file to write density information to.
     3. Add a density processor (recommended: `PedestrianDensityCountingProcessor`) to write into file from (2).
     4. Analyze output files: What is the max. observed density value?
3. Estimate how long it takes to evacuate 500 agents from Marienplatz and check your estimation against a simulation. Note: you may also increase the number of agents, but depending on your hardware the simulation may take a while. 
   * **Used scenario:** 03-Marienplatz-Evacuation
   * **Steps:**
     1. What is a reasonable location and shape of the source? 
     2. Place single or multiple targets? Note: Make sure all targets have the same Id and are set in the `source` JSON field `targetIds`. 
     3. How fast should the agents run? (`Topography -> attributesPedestrian -> [speedDistributionMean | speedDistributionStandardDeviation]`)
     4. Use the `PedestrianEvacuationTimeProcessor` to measure the evacuation time per pedestrian. What is the maximum and mean evacuation time?


