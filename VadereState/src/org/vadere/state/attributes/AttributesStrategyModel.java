package org.vadere.state.attributes;

import java.util.LinkedList;

public class AttributesStrategyModel extends Attributes {

    /**
     * Store the members of this class under this key in the JSON file.
     */
    public static final String JSON_KEY = "attributesStrategy";

    private boolean useStrategyModel;
    private String strategyModel;
    private LinkedList<String> arguments = new LinkedList<>();
    private LinkedList<Integer> requiredDataProcessorIds = new LinkedList<>();


    // Constructors
    public AttributesStrategyModel(){
        this.useStrategyModel = false;
        this.strategyModel = null;
        this.arguments = new LinkedList<>();
        this.requiredDataProcessorIds = new LinkedList<>();
    }

    public AttributesStrategyModel(boolean useStrategyModel, String strategyModel, LinkedList<String> arguments, LinkedList<Integer> requiredDataProcessorIds) {
        this.useStrategyModel = useStrategyModel;
        this.strategyModel = strategyModel;
        this.arguments = arguments;
        this.requiredDataProcessorIds = requiredDataProcessorIds;
    }

    // getter and setters
    public String getStrategyModel() {
        return strategyModel;
    }

    public void setStrategyModel(String strategyModel) {
        this.strategyModel = strategyModel;
    }

    public boolean isUseStrategyModel() {
        return useStrategyModel;
    }

    public void setUseStrategyModel(boolean useStrategyModel) {
        this.useStrategyModel = useStrategyModel;
    }

    public LinkedList<String> getArguments() {
        return arguments;
    }

    public void setArguments(LinkedList<String> arguments) {
        this.arguments = arguments;
    }

    public LinkedList<Integer> getRequiredDataProcessorIds() {
        return requiredDataProcessorIds;
    }

    public void setRequiredDataProcessorIds(LinkedList<Integer> requiredDataProcessorIds) {
        this.requiredDataProcessorIds = requiredDataProcessorIds;
    }
}



