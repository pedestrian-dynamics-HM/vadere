package org.vadere.simulator.control.psychology.cognition.models;

import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.Collection;

/**
 * The {@link SimpleCognitionModel} just passes the perceived stimulus to the
 * behavioral/locomotion layer without further processing.
 */
public class SimpleCognitionModel implements ICognitionModel {

    private Topography topography;

    @Override
    public void initialize(Topography topography) {
        this.topography = topography;
    }

    @Override
    public void update(Collection<Pedestrian> pedestrians) {
        for (Pedestrian pedestrian : pedestrians) {

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
