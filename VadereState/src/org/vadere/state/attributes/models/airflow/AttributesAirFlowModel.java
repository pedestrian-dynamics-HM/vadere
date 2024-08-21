package org.vadere.state.attributes.models.airflow;


import org.vadere.state.attributes.Attributes;

public class AttributesAirFlowModel extends Attributes {

    private String condaPath;

    private String condaEnv;

    private double gridSize;

    private double areaThreshold;

    private String inletSide;

    private double inletStart;

    private double inletEnd;

    private double inletVelocity;

    private String outletSide;

    private double outletStart;

    private double outletEnd;


    public AttributesAirFlowModel() {
        condaPath = "CONDA_EXE";
        condaEnv = "CONDA_ENV";
        gridSize = 0.5;
        areaThreshold = 0.1;
        inletSide = "bottom";
        inletStart = 0;
        inletEnd = 2;
        inletVelocity = 1;
        outletSide = "right";
        outletStart = 0;
        outletEnd = 2;
    }

    public AttributesAirFlowModel(double gridSize) {
        this.gridSize = gridSize;
    }

    public String getCondaPath() {
        return condaPath;
    }

    public String getCondaEnv() {
        return condaEnv;
    }

    public double getGridSize() {
        return gridSize;
    }

    public double getAreaThreshold() {
        return areaThreshold;
    }

    public String getInletSide() {
        return inletSide;
    }

    public double getInletStart() {
        return inletStart;
    }

    public double getInletEnd() {
        return inletEnd;
    }

    public double getInletVelocity() {
        return inletVelocity;
    }

    public String getOutletSide() {
        return outletSide;
    }

    public double getOutletStart() {
        return outletStart;
    }

    public double getOutletEnd() {
        return outletEnd;
    }
}
