package org.vadere.simulator.control.psychology.cognition;

import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootstepHistory;

import java.util.Collection;

public class CooperativeCognitionModel implements ICognitionModel {

    private Topography topography;

    @Override
    public void initialize(Topography topography) {
        this.topography = topography;
    }

    @Override
    public void update(Collection<Pedestrian> pedestrians) {
        for (Pedestrian pedestrian : pedestrians) {
            // TODO: Maybe, add following variables as attribute to new class "AttributesCognition".
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
