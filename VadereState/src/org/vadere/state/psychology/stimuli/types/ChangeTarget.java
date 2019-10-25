package org.vadere.state.psychology.stimuli.types;

import org.vadere.state.scenario.ScenarioElement;

import java.util.LinkedList;
import java.util.List;

/**
 * Class signals agents to change their targets.
 */
public class ChangeTarget extends Stimulus {

    // Member Variables
    private LinkedList<Integer> newTargetIds;

    // Constructors
    // Default constructor required for JSON de-/serialization.
    public ChangeTarget() { super(); }

    public ChangeTarget(double time) {
        super(time);
    }

    public ChangeTarget(double time, LinkedList<Integer> newTargetIds) {
        super(time);

        this.newTargetIds = newTargetIds;
    }

    // Getter
    public LinkedList<Integer> getNewTargetIds() { return newTargetIds; }

    // Setter
    public void setNewTargetIds(LinkedList<Integer> newTargetIds) {
        this.newTargetIds = newTargetIds;
    }

}
