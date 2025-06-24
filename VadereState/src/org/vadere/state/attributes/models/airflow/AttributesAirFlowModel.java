package org.vadere.state.attributes.models.airflow;


import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

@ModelAttributeClass
public class AttributesAirFlowModel extends Attributes {

    private String condaPath;

    private String condaEnv;

    private String pythonPath;

    private double gridSize;

    private double areaThreshold;

    private double inletVelocity;

    private ArrayList<AttributesInOutLet> inlets;

    private ArrayList<AttributesInOutLet> outlets;

    private ArrayList<Integer> blockingObstacles;

    private double onPeriod;

    private double offPeriod;

    private AttributesBounds bounds;

    public AttributesAirFlowModel() {
        condaPath = "CONDA_EXE";
        condaEnv = "CONDA_ENV";
        pythonPath = "VadereSimulator/src/org/vadere/simulator/models/airflow/python/navier_stokes.py";
        gridSize = 0.2;
        areaThreshold = 0.1;
        inletVelocity = 0.3;
        inlets = new ArrayList<>();
        inlets.add(new AttributesInOutLet("left", 1, 2));
        outlets = new ArrayList<>();
        outlets.add(new AttributesInOutLet("right", 1, 2));
        blockingObstacles = new ArrayList<>();
        onPeriod = 1.0;
        offPeriod = 0;
        bounds = new AttributesBounds();
    }

    public AttributesAirFlowModel(double gridSize, double areaThreshold, double inletVelocity,
                                  ArrayList<AttributesInOutLet> inlets, ArrayList<AttributesInOutLet> outlets,
                                  ArrayList<Integer> notBlockingObstacles, AttributesBounds bounds) {
        condaPath = "CONDA_EXE";
        condaEnv = "CONDA_ENV";
        pythonPath = "VadereSimulator/src/org/vadere/simulator/models/airflow/python/navier_stokes.py";
        this.gridSize = gridSize;
        this.areaThreshold = areaThreshold;
        this.inletVelocity = inletVelocity;
        this.inlets = inlets;
        this.outlets = outlets;
        this.blockingObstacles = notBlockingObstacles;
        onPeriod = 1.0;
        offPeriod = 0; 
        this.bounds = bounds;
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

    public String getPythonPath() { return pythonPath; }

    public double getGridSize() {
        return gridSize;
    }

    public double getAreaThreshold() {
        return areaThreshold;
    }

    public double getInletVelocity() {
        return inletVelocity;
    }

    public ArrayList<AttributesInOutLet> getInlets() {
        return inlets;
    }

    public ArrayList<AttributesInOutLet> getOutlets() {
        return outlets;
    }

    public ArrayList<Integer> getBlockingObstacles() {
        return blockingObstacles;
    }

    public double getOnPeriod() {
        return onPeriod;
    }

    public double getOffPeriod() {
        return offPeriod;
    }

    public void setOffPeriod(double period) {
        this.offPeriod = period;
    }

    public AttributesBounds getBounds() {
        return bounds;
    }

    public void setBounds(AttributesBounds bounds) {
        this.bounds = bounds;
    }
}
