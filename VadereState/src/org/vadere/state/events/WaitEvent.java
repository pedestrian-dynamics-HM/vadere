package org.vadere.state.events;

import org.vadere.state.scenario.ScenarioElement;

import java.util.List;

/**
 * Class can signal agents to wait - for instance at a red traffic light.
 */
public class WaitEvent extends Event {

    public WaitEvent(double time) {
        super(time);
    }

    public WaitEvent(double time, List<ScenarioElement> targets) {
        super(time, targets);
    }

}
