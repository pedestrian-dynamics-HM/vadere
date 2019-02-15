package org.vadere.state.events.types;

import org.vadere.state.scenario.ScenarioElement;

import java.util.List;

/**
 * Class signals agents a bang - for instance something exploded.
 */
public class BangEvent extends Event {

    // Default constructor required for JSON de-/serialization.
    public BangEvent() { super(); }

    public BangEvent(double time) {
        super(time);
    }

    public BangEvent(double time, List<ScenarioElement> targets) {
        super(time, targets);
    }

}
