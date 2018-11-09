# TopographyChecker Messages

## Overlapping ScenarioElements

|Type of Elemetns    |	TotalOvelrap/ contained in 	|  PartialOverlap  |
|--------------------|:----------------:|:----------------:|
|Obstacle / Obstacle | 	     WARN       |       ./.        |
|Obstacle / Source   |       ERROR      |       ERROR      |
|Obstacle / Target   |       ERROR      |       WARN       |
|Obstacle / Stairs   |       ERROR      |       WARN       |
|Obstacle / Ped.     |       ERROR      |       ERROR      |
|Source   / Source   |       WARN       |       WARN       |
|Source   / Target   |       WARN       |       WARN       |
|Source   / Stairs   |       WARN       |       WARN       |
|Target   / Target   |       WARN       |       WARN       |
|Target   / Stairs   |       WARN       |       WARN       |
|Stairs   / Stairs   |       ERROR      |       ERROR      |

Legend:

WARN Warning but simulation is possible
ERROR Error and simulation is not possible
./. Nothing to do all good


## Misc Tests

### Errors

- A Source has no targetId set but is setup to spawn pedestrians. Solution: Set targetId
- A Source has a targetId set but the target does not exist. Solution: Create target or remove unused targetId from Source.
- If the SpeedDistributionMean of a pedestrian ist not between min/max speed.


### Warnings

- A Source has no targetId set and does not spawn pedestrians. Solution: Possible error
- A Target is never used by any Source. Solution: Possible error and this will cost performance.
- The Stairs model only works for 'normal' one-step-stairs. If the tread is outside of
  the range of 10cm < x < 35cm. The simulation does not make sense.
- If the speed setup of a pedestrian is bigger then 12.0 m/s (world record). This is possible  an error.
