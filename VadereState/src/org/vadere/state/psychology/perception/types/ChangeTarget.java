package org.vadere.state.psychology.perception.types;

import org.apache.commons.math3.util.Precision;

import java.util.LinkedList;

/**
 * Class signals agents to change their targets.
 */
public class ChangeTarget extends Stimulus {

    // Member Variables
    private LinkedList<Integer> newTargetIds = new LinkedList<>();

    // Constructors
    // Default constructor required for JSON de-/serialization.
    public ChangeTarget() { super(); }

    public ChangeTarget(double time) {
        super(time);
    }

    public ChangeTarget(double time, double probability) {
        super(time, probability);
    }

    public ChangeTarget(double time, LinkedList<Integer> newTargetIds) {
        super(time);
        this.newTargetIds = newTargetIds;
    }

    public ChangeTarget(double time, double probability, LinkedList<Integer> newTargetIds) {
        super(time, probability);
        this.newTargetIds = newTargetIds;
    }

    public ChangeTarget(double time, double probability, LinkedList<Integer> newTargetIds, int stimulusId) {
        super(time, probability, stimulusId);
        this.newTargetIds = newTargetIds;
    }

    public ChangeTarget(double time,  LinkedList<Integer> newTargetIds, int stimulusId){
        super(time, stimulusId);
        this.newTargetIds = newTargetIds;
    }

    public ChangeTarget(ChangeTarget other) {
        super(other);

        newTargetIds = new LinkedList<>();
        newTargetIds.addAll(other.newTargetIds);
    }

    // Getter
    public LinkedList<Integer> getNewTargetIds() { return newTargetIds; }

    // Setter
    public void setNewTargetIds(LinkedList<Integer> newTargetIds) {
        this.newTargetIds = newTargetIds;
    }

    // Methods
    @Override
    public ChangeTarget clone() {
        return new ChangeTarget(this);
    }

    @Override
    public boolean equals(Object that){
        if(this == that) return true;
        if(!(that instanceof ChangeTarget)) return false;
        ChangeTarget thatChangeTarget = (ChangeTarget) that;
        return this.newTargetIds.equals(thatChangeTarget.getNewTargetIds());
    }

}
