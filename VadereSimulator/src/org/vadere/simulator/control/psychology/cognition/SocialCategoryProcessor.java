package org.vadere.simulator.control.psychology.cognition;

import org.vadere.simulator.control.psychology.perception.StimulusProcessor;
import org.vadere.state.psychology.cognition.SocialCategory;
import org.vadere.state.psychology.perception.types.Bang;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootstepHistory;

import java.util.Collection;

/**
 * The SocialCategoryProcessor class should provide logic to change the social
 * category of a pedestrian (e.g., change to cooperative if no movement is
 * possible for n steps).
 *
 * Watch out: The {@link StimulusProcessor} should be finished before using
 * methods in this class because, usually, first a stimulus is processed and
 * then pedestrians decide which behavior to follow. E.g., first a {@link Bang}
 * occurs and then a pedestrian decides to follow a {@link SocialCategory#TARGET_ORIENTED}
 * behavior.
 */
public class SocialCategoryProcessor {

    /** The social category may also depend on the surrounding environment. */
    private Topography topography;

    public SocialCategoryProcessor(Topography topography) {
        this.topography = topography;
    }

    public void setSocialCategoryOfPedestrian(Collection<Pedestrian> pedestrians, double simTimeInSec) {
        // TODO: Include more (pedestrian) attributes into decision process.

        for (Pedestrian pedestrian : pedestrians) {
            // TODO: Maybe, add following variables as attribute to "AttributesAgent".
            int requiredFootSteps = 2;
            double requiredSpeedInMetersPerSecondToBeCooperative = 0.05;

            FootstepHistory footstepHistory = pedestrian.getFootstepHistory();

            // Adapt category only if we have seen some footsteps in the past
            if (footstepHistory.size() >= requiredFootSteps) {
                if (footstepHistory.getAverageSpeedInMeterPerSecond() <= requiredSpeedInMetersPerSecondToBeCooperative) {
                    pedestrian.setSocialCategory(SocialCategory.COOPERATIVE);
                } else {
                    // TODO: Maybe, check if area directed to target is free for a step
                    //   (only then change to "TARGET_ORIENTED").
                    pedestrian.setSocialCategory(SocialCategory.TARGET_ORIENTED);
                }
            }
        }
    }
}
