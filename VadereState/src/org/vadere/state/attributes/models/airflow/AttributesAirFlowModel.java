package org.vadere.state.attributes.models.airflow;


import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

import java.util.ArrayList;

@ModelAttributeClass
public class AttributesAirFlowModel extends Attributes {

    private String condaPath;

    private String condaEnv;

    private String pythonPath;

    private double rectangularGridCellSize;

    private double maxTriangleEdgeLen;

    private double inletVelocity;

    private double reynoldsNumber;

    private ArrayList<AttributesInOutLet> inlets;

    private ArrayList<AttributesInOutLet> outlets;

    private ArrayList<Integer> blockingObstacles;

    private double onPeriod;

    private double offPeriod;

    private AttributesBounds bounds;

    public AttributesAirFlowModel() {
        condaPath = "CONDA_PATH";
        condaEnv = "CONDA_ENV";
        pythonPath = "VadereSimulator/src/org/vadere/simulator/models/airflow/python/navier_stokes.py";
        maxTriangleEdgeLen = 0.2;
        rectangularGridCellSize = 0.1;
        inletVelocity = 0.3;
        reynoldsNumber = 4000;
        inlets = new ArrayList<>();
        inlets.add(new AttributesInOutLet("west", 1, 1));
        outlets = new ArrayList<>();
        outlets.add(new AttributesInOutLet("east", 1, 1));
        blockingObstacles = new ArrayList<>();
        onPeriod = 1.0;
        offPeriod = 0;
        bounds = new AttributesBounds();
    }

    public AttributesAirFlowModel(double rectangularGridCellSize, double maxTriangleEdgeLen, double inletVelocity,
                                  double reynoldsNumber,
                                  ArrayList<AttributesInOutLet> inlets, ArrayList<AttributesInOutLet> outlets,
                                  ArrayList<Integer> blockingObstacles, AttributesBounds bounds) {
        condaPath = "CONDA_PATH";
        condaEnv = "CONDA_ENV";
        pythonPath = "VadereSimulator/src/org/vadere/simulator/models/airflow/python/navier_stokes.py";
        this.maxTriangleEdgeLen = maxTriangleEdgeLen;
        this.rectangularGridCellSize = rectangularGridCellSize;
        this.inletVelocity = inletVelocity;
        this.reynoldsNumber = reynoldsNumber;
        this.inlets = inlets;
        this.outlets = outlets;
        this.blockingObstacles = blockingObstacles;
        onPeriod = 1.0;
        offPeriod = 0; 
        this.bounds = bounds;
    }

    public AttributesAirFlowModel(double rectangularGridCellSize) {
        this.rectangularGridCellSize = rectangularGridCellSize;
    }

    public String getCondaPath() {
        return condaPath;
    }

    public String getCondaEnv() {
        return condaEnv;
    }

    public String getPythonPath() { return pythonPath; }

    public double getRectangularGridCellSize() {
        return rectangularGridCellSize;
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
