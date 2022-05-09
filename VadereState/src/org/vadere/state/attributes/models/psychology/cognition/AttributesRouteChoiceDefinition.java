package org.vadere.state.attributes.models.psychology.cognition;

import java.util.LinkedList;

public class AttributesRouteChoiceDefinition {

    private String instruction;
    private LinkedList<Integer> targetIds;
    private LinkedList<Double> targetProbabilities;

    public AttributesRouteChoiceDefinition(){
        instruction = "";
        targetIds = new LinkedList<>();
        targetProbabilities = new LinkedList<>();
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public LinkedList<Integer> getTargetIds() {
        return targetIds;
    }

    public void setTargetIds(LinkedList<Integer> targetIds) {
        this.targetIds = targetIds;
    }

    public LinkedList<Double> getTargetProbabilities() {
        return targetProbabilities;
    }

    public void setTargetProbabilities(LinkedList<Double> targetProbabilities) {
        this.targetProbabilities = targetProbabilities;
    }
}
