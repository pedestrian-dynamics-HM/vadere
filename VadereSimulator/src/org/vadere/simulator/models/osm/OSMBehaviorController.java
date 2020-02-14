package org.vadere.simulator.models.osm;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.simulator.models.potential.combinedPotentials.CombinedPotentialStrategy;
import org.vadere.simulator.models.potential.combinedPotentials.TargetRepulsionStrategy;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.ChangeTarget;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.psychology.perception.types.Threat;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A class to encapsulate the behavior of a single {@link PedestrianOSM}.
 *
 * This class can be used by {@link OptimalStepsModel} to react to
 * environmental stimuli (see {@link Stimulus}) and how an agent
 * has categorized itself in regard to other agents (see {@link SelfCategory}).
 *
 * For instance:
 * <pre>
 *     ...
 *     if (mostImportantStimulus instanceof Wait) {
 *         osmBehaviorController.wait()
 *     }
 * 	   ...
 * </pre>
 */
public class OSMBehaviorController {

    private static Logger logger = Logger.getLogger(OSMBehaviorController.class);

    public void makeStepToTarget(@NotNull final PedestrianOSM pedestrian, @NotNull final Topography topography) {
        // this can cause problems if the pedestrian desired speed is 0 (see speed adjuster)
        pedestrian.updateNextPosition();
        makeStep(pedestrian, topography, pedestrian.getDurationNextStep());
        pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
    }

    /**
     * Prepare move of pedestrian inside the topography. The pedestrian object already has the new
     * location (Vpoint to) stored within its position attribute. This method only informs the
     * topography object of the change in state.
     *
     * !IMPORTANT! this function calls movePedestrian which must be called ONLY ONCE  for each
     * pedestrian for each position. To  allow preformat selection of a pedestrian the managing
     * destructure is not idempotent (cannot be applied multiple time without changing result).
     *
     * @param topography 	manages simulation data
     * @param pedestrian	moving pedestrian. This object's position is already set.
     * @param stepTime		time in seconds used for the step.
     */
    public void makeStep(@NotNull final PedestrianOSM pedestrian, @NotNull final Topography topography, final double stepTime) {
        VPoint currentPosition = pedestrian.getPosition();
        VPoint nextPosition = pedestrian.getNextPosition();

        // start time
        double stepStartTime = pedestrian.getTimeOfNextStep();

        // end time
        double stepEndTime = pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep();

        assert stepEndTime >= stepStartTime && stepEndTime >= 0.0 && stepStartTime >= 0.0 : stepEndTime + "<" + stepStartTime;

        if (nextPosition.equals(currentPosition)) {
            pedestrian.setVelocity(new Vector2D(0, 0));

        } else {
            pedestrian.setPosition(nextPosition);
            synchronized (topography) {
                topography.moveElement(pedestrian, currentPosition);
            }

            // compute velocity by forward difference
            Vector2D pedVelocity = new Vector2D(nextPosition.x - currentPosition.x, nextPosition.y - currentPosition.y).multiply(1.0 / stepTime);
            pedestrian.setVelocity(pedVelocity);
        }

        // strides and foot steps have no influence on the simulation itself, i.e. they are saved to analyse trajectories
        pedestrian.getStrides().add(Pair.of(currentPosition.distance(nextPosition), stepStartTime));

        FootStep currentFootstep = new FootStep(currentPosition, nextPosition, stepStartTime, stepEndTime);
        pedestrian.getTrajectory().add(currentFootstep);
        pedestrian.getFootstepHistory().add(currentFootstep);
    }

	/**
	 * This operation undo the last foot step of an agent. This is required to resolve conflicts by the {@link org.vadere.simulator.models.osm.updateScheme.UpdateSchemeParallel}.
	 *
	 * @param pedestrian the agent
	 * @param topography the topography
	 */
	public void undoStep(@NotNull final PedestrianOSM pedestrian, @NotNull final Topography topography) {
	    FootStep footStep = pedestrian.getTrajectory().removeLast();
	    pedestrian.getFootstepHistory().removeLast();

	    pedestrian.setPosition(footStep.getStart());
	    synchronized (topography) {
		    topography.moveElement(pedestrian, footStep.getEnd());
	    }
	    pedestrian.setVelocity(new Vector2D(0, 0));
    }

    public void wait(PedestrianOSM pedestrian, Topography topography, double timeStepInSec) {
        double stepStartTime = pedestrian.getTimeOfNextStep();
        double stepEndTime = stepStartTime + timeStepInSec;

        /* TODO: Discuss with Bene how to create a "correct footstep to avoid an interpolation exception
            and to get the psychology status logged.
        System.out.println(String.format("Ped[%d]: startTime[%.2f], endTime[%.2f], time[%.2f]", pedestrian.getId(), stepStartTime, stepEndTime, pedestrian.getMostImportantStimulus().getTime()));

        assert stepEndTime >= stepStartTime && stepEndTime >= 0.0 && stepStartTime >= 0.0 : stepEndTime + "<" + stepStartTime;

        VPoint currentPosition = pedestrian.getPosition();
        VPoint nextPosition = currentPosition;

        pedestrian.getStrides().add(Pair.of(currentPosition.distance(nextPosition), stepStartTime));

        // Force a "FootStep" so that output processor is able to write out current "PsychologyStatus".
        FootStep currentFootstep = new FootStep(currentPosition, nextPosition, stepStartTime, stepEndTime);
        pedestrian.getTrajectory().add(currentFootstep);
        pedestrian.getFootstepHistory().add(currentFootstep);
        */

        pedestrian.setTimeOfNextStep(stepEndTime);
    }

    public void changeToTargetRepulsionStrategyAndIncreaseSpeed(PedestrianOSM pedestrian, Topography topography) {
        if (pedestrian.getThreatMemory().isLatestThreatUnhandled()) {
            Threat threat = pedestrian.getThreatMemory().getLatestThreat();
            Target threatOrigin = topography.getTarget(threat.getOriginAsTargetId());

            LinkedList<Integer> nextTarget = new LinkedList<>();
            nextTarget.add(threatOrigin.getId());

            pedestrian.setTargets(nextTarget);
            pedestrian.setCombinedPotentialStrategy(CombinedPotentialStrategy.TARGET_REPULSION_STRATEGY);

            // TODO: Maybe, sample speed-up from a distribution or define it as a configurable attribute.
            double escapeSpeed = pedestrian.getFreeFlowSpeed() * 2.0;
            pedestrian.setFreeFlowSpeed(escapeSpeed);

            pedestrian.getThreatMemory().setLatestThreatUnhandled(false);
        }
    }

    /**
     * In dangerous situation humans tend to escape to familiar places (safe zones).
     * A pedestrian selects the target which is closest to its source as safe zone.
     * Or if pedestrian has no target, select closest target as safe zone.
     *
     * TODO: Clarify with Gerta if this is really a plausible assumption for safe zones.
     *   An easier approach is to just use the closest target as safe zone.
     */
    public void changeTargetToSafeZone(PedestrianOSM pedestrian, Topography topography) {
        if (pedestrian.getCombinedPotentialStrategy() instanceof TargetRepulsionStrategy) {

            ScenarioElement searchPosition = (pedestrian.getSource() == null) ? pedestrian : pedestrian.getSource();
            Target closestTarget = findClosestTarget(topography, searchPosition, pedestrian.getThreatMemory().getLatestThreat());

            assert closestTarget != null;

            if (closestTarget != null) {
                pedestrian.setSingleTarget(closestTarget.getId(), false);
            }

            pedestrian.setCombinedPotentialStrategy(CombinedPotentialStrategy.TARGET_ATTRACTION_STRATEGY);
        }
    }

    private Target findClosestTarget(Topography topography, ScenarioElement scenarioElement, Threat threat) {
        VPoint sourceCentroid = scenarioElement.getShape().getCentroid();

        List<Target> sortedTargets = topography.getTargets().stream()
                .filter(target -> target.getId() != threat.getOriginAsTargetId())
                .sorted((target1, target2) -> Double.compare(
                        sourceCentroid.distance(target1.getShape().getCentroid()),
                        sourceCentroid.distance(target2.getShape().getCentroid())))
                .collect(Collectors.toList());

        Target closestTarget = (sortedTargets.isEmpty()) ? null : sortedTargets.get(0);

        return closestTarget;
    }

    public void changeTarget(PedestrianOSM pedestrian, Topography topography) {
        Stimulus mostImportantStimulus = pedestrian.getMostImportantStimulus();

        if (mostImportantStimulus instanceof ChangeTarget) {
            ChangeTarget changeTarget = (ChangeTarget) pedestrian.getMostImportantStimulus();
            pedestrian.setTargets(changeTarget.getNewTargetIds());
            pedestrian.setNextTargetListIndex(0);
        } else {
            logger.debug(String.format("Expected: %s, Received: %s",
                    ChangeTarget.class.getSimpleName(),
                    mostImportantStimulus.getClass().getSimpleName()));
        }

        // Set time of next step. Otherwise, the internal OSM event queue hangs endlessly.
        pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
    }

    @Nullable
    public PedestrianOSM findSwapCandidate(PedestrianOSM pedestrian, Topography topography) {
        // Agents with no targets don't want to swap places.
        if (pedestrian.hasNextTarget() == false) {
            return null;
        }

        List<Pedestrian> closestPedestrians = getClosestPedestriansWhichAreCloserToTarget(pedestrian, topography);

        if (closestPedestrians.size() > 0) {
            for (Pedestrian closestPedestrian : closestPedestrians) {
                if (closestPedestrian.hasNextTarget()) {
                    boolean closestPedIsCooperative = closestPedestrian.getSelfCategory() == SelfCategory.COOPERATIVE;
                    boolean walkingDirectionDiffers = false;

                    double angleInRadian = calculateAngleBetweenWalkingDirections(pedestrian, closestPedestrian, topography);

                    if (angleInRadian == -1 || Math.toDegrees(angleInRadian) > pedestrian.getAttributes().getWalkingDirectionSameIfAngleLessOrEqual()) {
                        walkingDirectionDiffers = true;
                    }

                    if (closestPedIsCooperative && walkingDirectionDiffers) {
                        return (PedestrianOSM)closestPedestrian;
                    }
                } else {
                    return (PedestrianOSM)closestPedestrian;
                }
            }
        }

        return null;
    }

    @NotNull
    private List<Pedestrian> getClosestPedestriansWhichAreCloserToTarget(PedestrianOSM pedestrian, Topography topography) {
        VPoint positionOfPedestrian = pedestrian.getPosition();

        List<Pedestrian> closestPedestrians = topography.getSpatialMap(Pedestrian.class)
                .getObjects(positionOfPedestrian, pedestrian.getAttributes().getSearchRadius());

        // Filter out "me" and pedestrians which are further away from target than "me".
        closestPedestrians = closestPedestrians.stream()
                .filter(candidate -> pedestrian.getId() != candidate.getId())
                .filter(candidate -> pedestrian.getTargetPotential(candidate.getPosition()) < pedestrian.getTargetPotential(pedestrian.getPosition()))
                .collect(Collectors.toList());

        // Sort by distance away from "me".
        closestPedestrians = closestPedestrians.stream()
                .sorted((pedestrian1, pedestrian2) ->
                        Double.compare(
                                positionOfPedestrian.distance(pedestrian1.getPosition()),
                                positionOfPedestrian.distance(pedestrian2.getPosition())
                        ))
                .collect(Collectors.toList());

        return closestPedestrians;
    }

    private double calculateAngleBetweenWalkingDirections(PedestrianOSM pedestrian1, Pedestrian pedestrian2, Topography topography) {
        double angleInRadian = -1;

        switch (pedestrian1.getAttributes().getWalkingDirectionCalculation()) {
            case BY_GRADIENT:
                angleInRadian = calculateAngleBetweenTargetGradients(pedestrian1, (PedestrianOSM)pedestrian2);
                break;
            case BY_TARGET_CENTER:
            case BY_TARGET_CLOSEST_POINT:
                angleInRadian = calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported calculation type: \"%s\"",
                        pedestrian1.getAttributes().getWalkingDirectionCalculation()));
        }

        return angleInRadian;
    }

    public double calculateAngleBetweenTargetGradients(PedestrianOSM pedestrian1, PedestrianOSM pedestrian2) {
        double angleInRadian = -1;

        Vector2D targetGradientPedestrian1 = pedestrian1.getTargetGradient(pedestrian1.getPosition());
        Vector2D targetGradientPedestrian2 = pedestrian2.getTargetGradient(pedestrian2.getPosition());

        double dotProduct = targetGradientPedestrian1.dotProduct(targetGradientPedestrian2);
        double multipliedMagnitudes = targetGradientPedestrian1.distanceToOrigin() * targetGradientPedestrian2.distanceToOrigin();

        angleInRadian = Math.acos(dotProduct / multipliedMagnitudes);

        return angleInRadian;
    }

    /**
     * Calculate the angle between the two vectors v1 and v2 where
     * v1 = (TargetPedestrian1 - pedestrian1) and v2 = (TargetPedestrian2 - pedestrian2):
     *
     * <pre>
     *     T2 o   o T1
     *        ^   ^
     *         \a/
     *          x
     *         / \
     *     P1 o   o P2
     *
     *     T1: target of pedestrian 1
     *     T2: target of pedestrian 2
     *     P1: pedestrian 1
     *     P2: pedestrian 2
     *     a : angle between the two vectors
     * </pre>
     *
     * This is required to decide if pedestrian1 and pedestrian2 can be swapped because they have different walking
     * directions.
     *
     * @return An angle between 0 and <i>pi</i> radian or -1 if at least one of the given pedestrians has no target.
     */
    public double calculateAngleBetweenTargets(Pedestrian pedestrian1, Pedestrian pedestrian2, Topography topography) {
        double angleInRadian = -1;

        if (pedestrian1.hasNextTarget() && pedestrian2.hasNextTarget()) {
            Target targetPed1 = topography.getTarget(pedestrian1.getNextTargetId());
            Target targetPed2 = topography.getTarget(pedestrian2.getNextTargetId());

            VPoint targetVectorPed1 = calculateVectorPedestrianToTarget(pedestrian1, targetPed1);
            VPoint targetVectorPed2 = calculateVectorPedestrianToTarget(pedestrian2, targetPed2);

            double dotProduct = targetVectorPed1.dotProduct(targetVectorPed2);
            double multipliedMagnitudes = targetVectorPed1.distanceToOrigin() * targetVectorPed2.distanceToOrigin();

            angleInRadian = Math.acos(dotProduct / multipliedMagnitudes);
        }

        return angleInRadian;
    }

    private VPoint calculateVectorPedestrianToTarget(Pedestrian pedestrian, Target target) {
        VPoint vectorPedestrianToTarget = null;

        if (pedestrian.getAttributes().getWalkingDirectionCalculation() == AttributesAgent.WalkingDirectionCalculation.BY_TARGET_CENTER) {
            vectorPedestrianToTarget = target.getShape().getCentroid().subtract(pedestrian.getPosition());
        } else if (pedestrian.getAttributes().getWalkingDirectionCalculation() == AttributesAgent.WalkingDirectionCalculation.BY_TARGET_CLOSEST_POINT) {
            VPoint closestTargetPoint = target.getShape().closestPoint(pedestrian.getPosition());
            vectorPedestrianToTarget = closestTargetPoint.subtract(pedestrian.getPosition());
        } else {
            throw new IllegalArgumentException(String.format("Unsupported angle calculation type: \"%s\"", pedestrian.getAttributes().getWalkingDirectionCalculation()));
        }

        return vectorPedestrianToTarget;
    }

    /**
     * Swap two pedestrians.
     *
     * Watch out: This method manipulates pedestrian2 which is contained in a queue
     * sorted by timeOfNextStep! The calling code must re-add pedestrian2 after
     * invoking this method.
     */
    public void swapPedestrians(PedestrianOSM pedestrian1, PedestrianOSM pedestrian2, Topography topography) {
        VPoint newPosition = pedestrian2.getPosition().clone();
        VPoint oldPosition = pedestrian1.getPosition().clone();

        pedestrian1.setNextPosition(newPosition);
        pedestrian2.setNextPosition(oldPosition);

        // Synchronize movement of both pedestrians
        double startTimeStep = pedestrian1.getTimeOfNextStep();
        double durationStep = Math.max(pedestrian1.getDurationNextStep(), pedestrian2.getDurationNextStep());
        double endTimeStep = startTimeStep + durationStep;

        // We interrupt the current footstep of pedestrian 2 to sync it with
        // pedestrian 1. It is only required for the sequential update scheme
        // since pedestrian 2 might have done some steps in this time step and
        // is ahead (with respect to the time) of pedestrian 1.
        // We remove those steps which is not a good solution!
        if(!pedestrian2.getTrajectory().isEmpty()) {
            pedestrian2.getTrajectory().adjustEndTime(startTimeStep);
        }

        pedestrian1.setTimeOfNextStep(startTimeStep);
        pedestrian2.setTimeOfNextStep(startTimeStep);

        makeStep(pedestrian1, topography, durationStep);
        makeStep(pedestrian2, topography, durationStep);

        pedestrian1.setTimeOfNextStep(endTimeStep);
        pedestrian2.setTimeOfNextStep(endTimeStep);
    }
}
