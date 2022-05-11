package org.vadere.state.psychology.perception.types;

import org.apache.commons.math3.util.Precision;

import java.util.LinkedList;

/**
 * Class signals agents to change their targets.
 *
 * This stimulus allows to script
 * <ul>
 *     <li>when agents change their target to a new target.</li>
 *     <li>how much agents should change their target to a new target.</li>
 * </ul>
 *
 * For instance, a stimulus description looks like this:
 * <ul>
 *     <li>allowedTimeDelta = 1.0</li>
 *     <li>changeRemainingPedestrians = true</li>
 *     <li>originalTargetIds = [1, 2]</li>
 *     <li>newTargetIds = [3, 4]</li>
 *     <li>simTimesToChangeTarget = [10.0, 20.0]</li>
 *     <li>totalAgentsToChangeTarget = [1, 2]</li>
 * </ul>
 *
 * On the cognition layer, this leads to following actions:
 * <ul>
 *     <li>At sim time 10.0 second, for 1 agent with target id 1 or 2 change target id to 3.</li>
 *     <li>At sim time 20.0 second, for 2 agents with target id 1 or 2 change target id to 4.</li>
 *     <li>At each sim time from "simTimesToChangeTarget", change also the target of all remaining agents
 *     (i.e., whose target id is not in list "originalTargetIds") if "changeRemainingPedestrians = true".
 *     For instance, at sim time 10.0 second the target id of agent a1 changed from 1 to 3 and at
 *     sim time 20.0 the target id is changed from 3 to 4 (since 3 is not in list [1, 2].</li>
 * </ul>
 *
 * Note: "allowedTimeDelta" is necessary to make sure that stimulus is injected properly. For instance, if
 * "simTimeStepLength = 0.4" the simulated times are [0.0, 0.4, 0.8, 1.2, ...]. If "ChangeTargetScripted" should
 * be injected at "start = 1.0" and "end = 1.1" this would mean it could not be injected because the
 * "simTimeStepLength" is too coarse. Setting "allowedTimeDelta = 0.2" causes that the stimulus is
 * injected at "simTime = 1.2".
 */
public class ChangeTargetScripted extends Stimulus {

    // Member Variables
    private double allowedTimeDelta;
    private boolean changeRemainingPedestrians;
    private LinkedList<Integer> originalTargetIds = new LinkedList<>();
    private LinkedList<Integer> newTargetIds = new LinkedList<>();
    private LinkedList<Double> simTimesToChangeTarget = new LinkedList<>();
    private LinkedList<Integer> totalAgentsToChangeTarget = new LinkedList<>();

    // Constructors
    // Default constructor required for JSON de-/serialization.
    public ChangeTargetScripted() {
        super();
    }

    public ChangeTargetScripted(double time) {
        super(time);
    }

    public ChangeTargetScripted(double time, double probability) {
        super(time, probability);
    }

    public ChangeTargetScripted(double time, LinkedList<Integer> newTargetIds) {
        super(time);
        this.newTargetIds = newTargetIds;
    }

    public ChangeTargetScripted(double time, LinkedList<Integer> newTargetIds, int id) {
        super(time, id);
        this.newTargetIds = newTargetIds;
    }

    public ChangeTargetScripted(ChangeTargetScripted other) {
        super(other);

        allowedTimeDelta = other.getAllowedTimeDelta();

        newTargetIds = new LinkedList<>();
        newTargetIds.addAll(other.newTargetIds);

        simTimesToChangeTarget = new LinkedList<>();
        simTimesToChangeTarget.addAll(other.simTimesToChangeTarget);

        totalAgentsToChangeTarget = new LinkedList<>();
        totalAgentsToChangeTarget.addAll(other.totalAgentsToChangeTarget);
    }

    // Getter
    public double getAllowedTimeDelta() {
        return allowedTimeDelta;
    }

    public boolean getChangeRemainingPedestrians() { return changeRemainingPedestrians; }

    public LinkedList<Integer> getOriginalTargetIds() {
        return originalTargetIds;
    }

    public LinkedList<Integer> getNewTargetIds() { return newTargetIds; }

    public LinkedList<Double> getSimTimesToChangeTarget() {
        return simTimesToChangeTarget;
    }

    public LinkedList<Integer> getTotalAgentsToChangeTarget() {
        return totalAgentsToChangeTarget;
    }

    // Setter
    public void setAllowedTimeDelta(double allowedTimeDelta) {
        this.allowedTimeDelta = allowedTimeDelta;
    }

    public void setChangeRemainingPedestrians(boolean changeRemainingPedestrians) {
        this.changeRemainingPedestrians = changeRemainingPedestrians;
    }

    public void setOriginalTargetIds(LinkedList<Integer> originalTargetIds) {
        this.originalTargetIds = originalTargetIds;
    }

    public void setNewTargetIds(LinkedList<Integer> newTargetIds) {
        this.newTargetIds = newTargetIds;
    }

    public void setSimTimesToChangeTarget(LinkedList<Double> simTimesToChangeTarget) {
        this.simTimesToChangeTarget = simTimesToChangeTarget;
    }
    public void setTotalAgentsToChangeTarget(LinkedList<Integer> totalAgentsToChangeTarget) {
        this.totalAgentsToChangeTarget = totalAgentsToChangeTarget;
    }

    // Methods
    @Override
    public ChangeTargetScripted clone() {
        return new ChangeTargetScripted(this);
    }

    @Override
    public boolean equals(Object that){
        if(this == that) return true;
        if(!(that instanceof ChangeTargetScripted)) return false;
        ChangeTargetScripted thatChangeTarget = (ChangeTargetScripted) that;
        return this.newTargetIds.equals(thatChangeTarget.getNewTargetIds());
    }

}
