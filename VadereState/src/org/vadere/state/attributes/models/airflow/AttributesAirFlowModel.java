package org.vadere.state.attributes.models.airflow;


import org.vadere.state.attributes.Attributes;

public class AttributesAirFlowModel extends Attributes {


    private double gridSize;

    private String condaPath;

    private String condaEnv;

    public AttributesAirFlowModel() {
        gridSize = 0.5;
        condaPath = System.getProperty("user.home") + "/conda";
        condaEnv = "CONDA_ENV";
    }

    public AttributesAirFlowModel(double gridSize) {
        this.gridSize = gridSize;
    }

    public double getGridSize() {
        return gridSize;
    }

    public String getCondaPath() {
        return condaPath;
    }

    public String getCondaEnv() {
        return condaEnv;
    }

}
