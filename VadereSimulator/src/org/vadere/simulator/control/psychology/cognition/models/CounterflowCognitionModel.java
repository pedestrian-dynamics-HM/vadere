package org.vadere.simulator.control.psychology.cognition.models;

import org.vadere.simulator.utils.topography.TopographyHelper;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.Collection;

/**
 * The {@link CounterflowCognitionModel} enables a pedestrian to evade if
 * counter-flowing agents are detected. The locomotion layer must implemenet
 * the specific evasion behavior (e.g. tangentially).
 *
 * Note: Maybe, combine this behavior also with {@link SelfCategory#COOPERATIVE} to avoid jams.
 */
public class CounterflowCognitionModel implements ICognitionModel {

    private Topography topography;

    @Override
    public void initialize(Topography topography) {
        this.topography = topography;
    }

    @Override
    public void update(Collection<Pedestrian> pedestrians) {
        for (Pedestrian pedestrian : pedestrians) {
            pedestrian.setSelfCategory(SelfCategory.TARGET_ORIENTED);

            if (pedestrian.hasNextTarget()) {
                Pedestrian neighborCloserToTarget = TopographyHelper.getNeighborCloserToTargetCentroid(pedestrian, topography);

                if (neighborCloserToTarget != null) {
                    if (TopographyHelper.walkingDirectionDiffers(pedestrian, neighborCloserToTarget, topography)) {
                        pedestrian.setSelfCategory(SelfCategory.EVADE);
                    } else if (neighborCloserToTarget.getSelfCategory() == SelfCategory.EVADE) { // Imitate behavior
                        pedestrian.setSelfCategory(SelfCategory.EVADE);
                    }
                }
            }
        }
    }

}
