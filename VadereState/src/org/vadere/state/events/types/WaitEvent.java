package org.vadere.state.events.types;

import org.vadere.state.scenario.ScenarioElement;

import java.util.List;

/**
 * Class can signal agents to wait - for instance at a red traffic light.
 */
public class WaitEvent extends Event {

    // Default constructor required for JSON de-/serialization.
    public WaitEvent() { super(); }

    public WaitEvent(double time) {
        super(time);
    }

    public WaitEvent(double time, List<ScenarioElement> targets) {
        super(time, targets);
    }

}
