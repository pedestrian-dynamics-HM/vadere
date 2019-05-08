package org.vadere.state.events.types;

import org.vadere.state.scenario.ScenarioElement;

import java.util.List;

/**
 * Class representing an elapsed time step.
 *
 * The class uses the inherited "time" to represent the current time step.
 */
public class ElapsedTimeEvent extends Event {

    // Default constructor required for JSON de-/serialization.
    public ElapsedTimeEvent() { super(); }

    public ElapsedTimeEvent(double time) {
        super(time);
    }

    public ElapsedTimeEvent(double time, List<ScenarioElement> targets) {
        super(time, targets);
    }

}
