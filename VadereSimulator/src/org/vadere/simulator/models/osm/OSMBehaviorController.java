package org.vadere.simulator.models.osm;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.simulator.models.potential.combinedPotentials.CombinedPotentialStrategy;
import org.vadere.simulator.models.potential.combinedPotentials.TargetRepulsionStrategy;
import org.vadere.simulator.utils.topography.TopographyHelper;
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

    // Static Variables
    private static Logger logger = Logger.getLogger(OSMBehaviorController.class);

    // Methods
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

    /**
     * Maximize distance to the threat (a threat) and increase speed.
     *
     * Watch out: The focus is the behavioral change here and not the exact speed-up. The exact speed-up factor
     * requires empirical data.
     *
     * In future: Requires data for calibration.
     *
     * @param pedestrian The pedestrian which escapes from a {@link Threat}.
     * @param topography The topography which is used to derive the location of the {@link Threat}.
     */
    public void changeToTargetRepulsionStrategyAndIncreaseSpeed(PedestrianOSM pedestrian, Topography topography) {
        if (pedestrian.getThreatMemory().isLatestThreatUnhandled()) {
            Threat threat = pedestrian.getThreatMemory().getLatestThreat();
            Target threatOrigin = topography.getTarget(threat.getOriginAsTargetId());

            LinkedList<Integer> nextTarget = new LinkedList<>();
            nextTarget.add(threatOrigin.getId());

            pedestrian.setTargets(nextTarget);
            pedestrian.setCombinedPotentialStrategy(CombinedPotentialStrategy.TARGET_REPULSION_STRATEGY);

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
     * Watch out: This is our current definition of a safe zone. Maybe, this mus be
     * adjusted in the future when more empirical data about safe zones is available.
     */
    public void changeTargetToSafeZone(PedestrianOSM pedestrian, Topography topography) {
        //if (pedestrian.getCombinedPotentialStrategy() instanceof TargetRepulsionStrategy) {

            ScenarioElement searchPosition = (pedestrian.getSource() == null) ? pedestrian : pedestrian.getSource();
            Target closestTarget = TopographyHelper.findClosestTargetToSource(topography, searchPosition, pedestrian.getThreatMemory().getLatestThreat());

            assert closestTarget != null;

            if (closestTarget != null) {
                pedestrian.setSingleTarget(closestTarget.getId(), false);
            }

            pedestrian.setCombinedPotentialStrategy(CombinedPotentialStrategy.TARGET_ATTRACTION_STRATEGY);
        //}
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

        List<Pedestrian> neighborsCloserToTarget = TopographyHelper.getNeighborsCloserToTarget(pedestrian, topography);

        if (neighborsCloserToTarget.size() > 0) {
            for (Pedestrian neighbor : neighborsCloserToTarget) {
                if (neighbor.hasNextTarget()) {
                    boolean neighborIsCooperative = neighbor.getSelfCategory() == SelfCategory.COOPERATIVE;
                    boolean walkingDirectionDiffers = TopographyHelper.walkingDirectionDiffers(pedestrian, neighbor, topography);

                    if (neighborIsCooperative && walkingDirectionDiffers) {
                        return (PedestrianOSM)neighbor;
                    }
                } else {
                    return (PedestrianOSM)neighbor;
                }
            }
        }

        return null;
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

    public void changeRepulsion(PedestrianOSM pedestrian){
        pedestrian.setCombinedPotentialStrategy(CombinedPotentialStrategy.PEDESTRIAN_REPULSION_STRATEGY);
    }



}
