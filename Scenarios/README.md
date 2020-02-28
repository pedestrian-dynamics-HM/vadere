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

1. In the first example, you should:
   - Get familiar with the GUI.
   - Compare different locomotion models.
     * **Used scenarios:**
       - 01-MinimalExample-OptimalStepsModel
       - 02-MinimalExample-GradientNavigationModel
     * **Steps:**
       1. Run both simlations.
       2. Compare both simulations:
          - What are the effects of different locomotion models?
          - TODO: Ask more questions to inspirate particpants!
2. In the second example, you should change a simulation parameter, the `PersonalSpace`, and check the consequences.
   * **Used scenario:** 03-PersonalSpace-Adjustment
   * **Steps:**
     1. GUI: `Model -> pedPotential...SpaceWidth`
3. In the third example, you should measure the density.
   * **Used scenario:** 04-Density-Measurement
   * **Steps:**
     1. GUI: `Data output`.
     2. Add a file to write output to.
     3. Add a density processor to write into this file.
     4. Analyze output file and compare output with other groups.
4. In the fourth example, you should estimate how long it takes to evacuate 1000 pedestrians from Marienplatz and check your estimation against a simulation run.
   * **Used scenario:** 05-Marienplatz-Evacuation
