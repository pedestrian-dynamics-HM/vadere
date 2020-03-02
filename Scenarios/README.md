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

1. Get started in the first example and compare two different models in Vadere
   * **Used scenarios:**
     - 01-MinimalExample-OptimalStepsModel
     - 01-MinimalExample-GradientNavigationModel
   * **Steps:**
     1. Get familiar with the GUI
     2. Compare different locomotion models by running both simlations, w and play around!
     3. Compare both simulations in the PosVis (see `Output files` section in the GUI)
        - How do the trajectories between OSM and GNM?
        - TODO: more questions?
2. In the second example, you should measure the density.
   * **Used scenario:** 02-Density-Measurement
   * **Steps:**
     1. GUI: `Data output`.
     2. Add a file to write output to.
     3. Add a density processor (recommended: `PedestrianDensityCountingProcessor`) to write into this file.
     4. Analyze output file and compare output with other groups.
3. Estimate how long it takes to evacuate 1000 (or more?) pedestrians from Marienplatz and check your estimation against a simulation run.
   * **Used scenario:** 03-Marienplatz-Evacuation
   * **Steps:**
     1. What are reasonable locations and shapes of source(s)? NOTE: Configure your target ids in the JSON!
     2. How do you place your (single /multiple) targets?
     3. How fast can people run? (`Topography -> attributesPedestrian -> [speedDistributionMean | speedDistributionStandardDeviation]`)
     4. Use the `PedestrianEvacuationTimeProcessor` to measure the evacuation time

