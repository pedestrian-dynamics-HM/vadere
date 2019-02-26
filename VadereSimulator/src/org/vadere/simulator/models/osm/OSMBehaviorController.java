package org.vadere.simulator.models.osm;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.potential.combinedPotentials.CombinedPotentialStrategy;
import org.vadere.state.events.types.BangEvent;
import org.vadere.state.events.types.Event;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.LinkedList;

/**
 * A class to encapsulate the behavior of a single {@link PedestrianOSM}.
 *
 * This class can be used by {@link OptimalStepsModel} to react on events.
 *
 * For instance:
 * <pre>
 *     ...
 *     if (mostImportantEvent instanceof WaitEvent) {
 *         osmBehaviorController.wait()
 *     }
 * 	   ...
 * </pre>
 */
public class OSMBehaviorController {

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
        double timeOfNextStep = pedestrian.getTimeOfNextStep();

        // end time
        double entTimeOfStep = pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep();

        if (nextPosition.equals(currentPosition)) {
            pedestrian.setTimeCredit(0);
            pedestrian.setVelocity(new Vector2D(0, 0));

        } else {
            pedestrian.setTimeCredit(pedestrian.getTimeCredit() - pedestrian.getDurationNextStep());

            pedestrian.setPosition(nextPosition);
            synchronized (topography) {
                topography.moveElement(pedestrian, pedestrian.getPosition());
            }

            // compute velocity by forward difference
            Vector2D pedVelocity = new Vector2D(nextPosition.x - currentPosition.x, nextPosition.y - currentPosition.y).multiply(1.0 / stepTime);
            pedestrian.setVelocity(pedVelocity);
        }

        /**
         * strides and foot steps have no influence on the simulation itself, i.e. they are saved to analyse trajectories
         */
        pedestrian.getStrides().add(Pair.of(currentPosition.distance(nextPosition), timeOfNextStep));
        pedestrian.getFootSteps().add(new FootStep(currentPosition, nextPosition, timeOfNextStep, entTimeOfStep));
    }

    public void wait(PedestrianOSM pedestrian) {
        pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
    }

    // Watch out: A bang event changes only the "CombinedPotentialStrategy".
    // I.e., a new target is set for the agent. The agent does not move here!
    // Therefore, trigger only a single bang event and then use "ElapsedTimeEvent" afterwards
    // to let the agent walk.
    public void reactToBang(PedestrianOSM pedestrian, Topography topography) {
        Event mostImportantEvent = pedestrian.getMostImportantEvent();

        if (mostImportantEvent instanceof BangEvent) {
            BangEvent bangEvent = (BangEvent) pedestrian.getMostImportantEvent();
            Target bangOrigin = topography.getTarget(bangEvent.getOriginAsTargetId());

            LinkedList<Integer> nextTarget = new LinkedList<>();
            nextTarget.add(bangOrigin.getId());

            pedestrian.setTargets(nextTarget);
            pedestrian.setCombinedPotentialStrategy(CombinedPotentialStrategy.TARGET_DISTRACTION_STRATEGY);
        } else {
            // TODO: Maybe, log to console.
        }
    }

}
