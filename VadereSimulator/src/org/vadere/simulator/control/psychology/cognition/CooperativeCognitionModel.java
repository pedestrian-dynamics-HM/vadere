package org.vadere.simulator.control.psychology.cognition;

import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootstepHistory;

import java.util.Collection;

/**
 * The {@link CooperativeCognitionModel} makes a pedestrian cooperative if its
 * average speed falls below a certain threshold. I.e., usually the agent
 * could not move for some time steps. For example, in case of other
 * counter-flowing agents.
 *
 * {@link SelfCategory#COOPERATIVE} should motivate pedestrians to swap places
 * instead of blindly walking to a target and colliding with other pedestrians.
 */
public class CooperativeCognitionModel implements ICognitionModel {

    private Topography topography;

    @Override
    public void initialize(Topography topography) {
        this.topography = topography;
    }

    @Override
    public void update(Collection<Pedestrian> pedestrians) {
        for (Pedestrian pedestrian : pedestrians) {
            // TODO: Maybe, add following variables as attributes to new class "AttributesCognition".
            int requiredFootSteps = 2;
            double requiredSpeedInMetersPerSecondToBeCooperative = 0.05;

            FootstepHistory footstepHistory = pedestrian.getFootstepHistory();

            // Adapt category only if we have seen some footsteps in the past
            if (footstepHistory.size() >= requiredFootSteps) {
                if (footstepHistory.getAverageSpeedInMeterPerSecond() <= requiredSpeedInMetersPerSecondToBeCooperative) {
                    pedestrian.setSelfCategory(SelfCategory.COOPERATIVE);
                } else {
                    // TODO: Maybe, check if area directed to target is free for a step
                    //   (only then change to "TARGET_ORIENTED").
                    pedestrian.setSelfCategory(SelfCategory.TARGET_ORIENTED);
                }
            }
        }
    }
}
