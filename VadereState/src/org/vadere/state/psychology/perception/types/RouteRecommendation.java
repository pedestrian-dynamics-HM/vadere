package org.vadere.state.psychology.perception.types;

import org.apache.commons.math3.util.Precision;

import java.util.LinkedList;

/**
 * Class that recommends agents targets.
 */
public class RouteRecommendation extends StimuliWrapper {

    // Member Variables
    private String instruction;
    private LinkedList<Integer> newTargetIds = new LinkedList<>();
    private LinkedList<Double> targetProbabilities = new LinkedList<>();

    // Constructors
    // Default constructor required for JSON de-/serialization.
    public RouteRecommendation() { super();
    }


    public RouteRecommendation(double time, LinkedList<Integer> newTargetIds, LinkedList<Double> targetProbabilities, String instruction) {
        super(time);
        this.newTargetIds = newTargetIds;
        this.targetProbabilities = targetProbabilities;
        this.instruction = instruction;
    }


    public RouteRecommendation(RouteRecommendation other) {
        super(other);

        newTargetIds = new LinkedList<>();
        newTargetIds.addAll(other.newTargetIds);

        targetProbabilities = new LinkedList<>();
        targetProbabilities.addAll(other.targetProbabilities);
        instruction = other.getInstruction();
    }

    // Getter
    public LinkedList<Integer> getNewTargetIds() { return newTargetIds; }

    // Setter
    public void setNewTargetIds(LinkedList<Integer> newTargetIds) {
        this.newTargetIds = newTargetIds;
    }

    // Methods

    public LinkedList<Double> getTargetProbabilities() {
        return targetProbabilities;
    }

    public void setTargetProbabilities(LinkedList<Double> targetProbabilities) {
        this.targetProbabilities = targetProbabilities;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }


    @Override
    public RouteRecommendation clone() {
        return new RouteRecommendation(this);
    }

    @Override
    public boolean equals(Object that){
        if(this == that) return true;
        if(!(that instanceof RouteRecommendation)) return false;
        RouteRecommendation that1 = (RouteRecommendation) that;
        boolean isProb = Precision.equals(this.perceptionProbability, that1.getPerceptionProbability(), Double.MIN_VALUE);
        boolean isInstructionEqual = this.instruction.equals(that1.getInstruction());
        boolean areSubProbsEqual = this.targetProbabilities.equals(that1.getTargetProbabilities());
        boolean isTargetIdsEqual = this.newTargetIds.equals(that1.getNewTargetIds());
        return isProb && isInstructionEqual && areSubProbsEqual && isTargetIdsEqual;
    }

    public LinkedList<Stimulus> unpackStimuli(){
        LinkedList<Stimulus> changeTargets = new LinkedList<>();

        if (this.newTargetIds.size() != this.targetProbabilities.size()){
            throw new RuntimeException("RouteRecommendation stimulus. Number .. ");
        }

        for (int i = 0; i < this.newTargetIds.size(); i++) {
            LinkedList<Integer> targetIds = new LinkedList();
            targetIds.add(this.newTargetIds.get(i));

            double probability = this.targetProbabilities.get(i);
            ChangeTarget changeTarget = new ChangeTarget(time, probability*this.perceptionProbability, targetIds);
            changeTargets.add(changeTarget);
        }

        return changeTargets;
    }






}
