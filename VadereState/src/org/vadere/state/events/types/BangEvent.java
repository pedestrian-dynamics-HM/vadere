package org.vadere.state.events.types;

import org.vadere.state.scenario.ScenarioElement;

import java.util.List;

/**
 * Class signals agents a bang - for instance something exploded.
 *
 * This event holds one additional information: a target id
 * which represents the origin of the bang.
 */
public class BangEvent extends Event {

    private int originAsTargetId = -1;

    // Default constructor required for JSON de-/serialization.
    public BangEvent() { super(); }

    public BangEvent(double time) {
        super(time);
    }

    public BangEvent(double time, List<ScenarioElement> targets) {
        super(time, targets);
    }

    public BangEvent(double time, List<ScenarioElement> targets, int originAsTargetId) {
        super(time, targets);

        this.originAsTargetId = originAsTargetId;
    }

    public int getOriginAsTargetId() { return originAsTargetId; }

    public void setOriginAsTargetId(int originAsTargetId) { this.originAsTargetId = originAsTargetId; }

}
