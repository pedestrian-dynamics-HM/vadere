package org.vadere.simulator.control.cognition;

import org.vadere.state.events.types.BangEvent;
import org.vadere.state.behavior.SalientBehavior;
import org.vadere.state.events.types.Event;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.Collection;
import java.util.List;

/**
 * The SalientBehaviorCognition class should provide logic to change the salient behavior of a pedestrian
 * (e.g., change to cooperative behavior when no movement is possible for n steps).
 *
 * Watch out: The {@link EventCognition} should be finished before using methods in this class because usually
 * first an event occurs and then pedestrians decide about their behavior. E.g., first a {@link BangEvent} occurs
 * and then a pedestrian decides to follow a {@link SalientBehavior#COOPERATIVE} behavior.
 */
public class SalientBehaviorCognition {

    /** The salient behavior depends also on the surrounding environment. */
    private Topography topography;

    public SalientBehaviorCognition(Topography topography) {
        this.topography = topography;
    }

    public void setSalientBehaviorForPedestrians(Collection<Pedestrian> pedestrians, double simTimeInSec) {
        for (Pedestrian pedestrian : pedestrians) {
            // TODO: Set salient behavior for each pedestrian individually based on the most important event and/or if
            //   the pedestrian could not move for several time steps.

            // TODO: Use pedestrian.getFootSteps() to calculate average velocity for last
        }
    }
}
