package org.vadere.state.attributes;

import java.util.Objects;

public class AttributesStrategyModel extends Attributes {

    /**
     * Store the members of this class under this key in the JSON file.
     */
    public static final String JSON_KEY = "attributesStrategy";

    private boolean useStrategyModel;
    private String strategyModel;

    // Constructors
    public AttributesStrategyModel(){
        this.useStrategyModel = false;
        this.strategyModel = null;
    }

    public AttributesStrategyModel(boolean useStrategyModel, String strategyModel) {
        this.useStrategyModel = useStrategyModel;
        this.strategyModel = strategyModel;
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
}



