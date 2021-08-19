package org.vadere.simulator.control.psychology.cognition.models;

import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootstepHistory;

import java.util.Collection;

/**
 * The {@link CooperativeAndOtherBehaviorsModel} makes a pedestrian cooperative if its
 * average speed falls below a certain threshold. I.e., usually the agent
 * could not move for some time steps. For example, in case of other
 * counter-flowing agents.
 *
 * {@link SelfCategory#COOPERATIVE} should motivate pedestrians to swap places
 * instead of blindly walking to a target and colliding with other pedestrians.
 */
public class CooperativeAndOtherBehaviorsModel implements ICognitionModel {

    private Topography topography;

    @Override
    public void initialize(Topography topography) {
        this.topography = topography;
    }

    @Override
    public void update(Collection<Pedestrian> pedestrians) {
        for (Pedestrian pedestrian : pedestrians) {
            if (pedestrianCannotMove(pedestrian)) {
                pedestrian.setSelfCategory(SelfCategory.COOPERATIVE);
            } else {

                Stimulus stimulus = pedestrian.getMostImportantStimulus();
                SelfCategory nextSelfCategory;

                if (stimulus instanceof ChangeTarget) {
                    nextSelfCategory = SelfCategory.CHANGE_TARGET;
                } else if (stimulus instanceof Threat) {
                    nextSelfCategory = SelfCategory.THREATENED;
                } else if (stimulus instanceof Wait || stimulus instanceof WaitInArea) {
                    nextSelfCategory = SelfCategory.WAIT;
                } else if (stimulus instanceof ElapsedTime) {
                    nextSelfCategory = SelfCategory.TARGET_ORIENTED;
                } else {
                    throw new IllegalArgumentException(String.format("Stimulus \"%s\" not supported by \"%s\"",
                            stimulus.getClass().getSimpleName(),
                            this.getClass().getSimpleName()));
                }

                pedestrian.setSelfCategory(nextSelfCategory);

            }
        }
    }

    private boolean pedestrianCannotMove(Pedestrian pedestrian) {
        boolean cannotMove = false;

        FootstepHistory footstepHistory = pedestrian.getFootstepHistory();
        int requiredFootSteps = 2;

        if (footstepHistory.size() >= requiredFootSteps
                && footstepHistory.getAverageSpeedInMeterPerSecond() <= 0.05) {
            cannotMove = true;
        }

        return cannotMove;
    }
}
