package org.vadere.state.events;

import org.vadere.state.scenario.ScenarioElement;

import java.util.List;

/**
 * Class representing an elapsed time step.
 *
 * The class uses the inherited "time" represent the current time step.
 */
public class ElapsedTimeEvent extends Event {

    public ElapsedTimeEvent(double time) {
        super(time);
    }

    public ElapsedTimeEvent(double time, List<ScenarioElement> targets) {
        super(time, targets);
    }

}
