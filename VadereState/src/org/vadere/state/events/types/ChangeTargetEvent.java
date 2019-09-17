package org.vadere.state.events.types;

import org.vadere.state.scenario.ScenarioElement;

import java.util.LinkedList;
import java.util.List;

/**
 * Class signals agents to change their targets.
 */
public class ChangeTargetEvent extends Event {

    private LinkedList<Integer> newTargetIds;

    // Default constructor required for JSON de-/serialization.
    public ChangeTargetEvent() { super(); }

    public ChangeTargetEvent(double time) {
        super(time);
    }

    public ChangeTargetEvent(double time, List<ScenarioElement> targets) {
        super(time, targets);
    }

    public ChangeTargetEvent(double time, LinkedList<Integer> newTargetIds) {
        super(time);

        this.newTargetIds = newTargetIds;
    }

    public LinkedList<Integer> getNewTargetIds() { return newTargetIds; }

    public void setNewTargetIds(LinkedList<Integer> newTargetIds) {
        this.newTargetIds = newTargetIds;
    }

}
