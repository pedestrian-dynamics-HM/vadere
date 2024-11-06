package org.vadere.state.attributes.models.airflow;


import org.vadere.state.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

public class AttributesAirFlowModel extends Attributes {

    private String condaPath;

    private String condaEnv;

    private double gridSize;

    private double areaThreshold;

    private double inletVelocity;

    private ArrayList<AttributesInOutLet> inlets;

    private ArrayList<AttributesInOutLet> outlets;

    public AttributesAirFlowModel() {
        condaPath = "CONDA_EXE";
        condaEnv = "CONDA_ENV";
        gridSize = 0.5;
        areaThreshold = 0.1;
        inletVelocity = 1;
        inlets = new ArrayList<>();
        outlets = new ArrayList<>();
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

    public double getInletVelocity() {
        return inletVelocity;
    }

    public List<AttributesInOutLet> getInlets() {
        return inlets;
    }

    public List<AttributesInOutLet> getOutlets() {
        return outlets;
    }
}
