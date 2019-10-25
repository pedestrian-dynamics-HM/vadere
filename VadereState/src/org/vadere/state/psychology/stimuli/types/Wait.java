package org.vadere.state.psychology.stimuli.types;

import org.vadere.state.scenario.ScenarioElement;

import java.util.List;

/**
 * Class can signal agents to wait - for instance at a red traffic light.
 */
public class Wait extends Stimulus {

    // Default constructor required for JSON de-/serialization.
    public Wait() { super(); }

    public Wait(double time) {
        super(time);
    }

}
