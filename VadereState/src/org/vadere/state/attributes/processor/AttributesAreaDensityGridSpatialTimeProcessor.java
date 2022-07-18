package org.vadere.state.attributes.processor;

public class AttributesAreaDensityGridSpatialTimeProcessor extends AttributesProcessor {

    double cellSize = 5.0;

    double T = 1;

    private int pedestrianTrajectoryProcessorId;

    public int getPedestrianTrajectoryProcessorId() {
        return pedestrianTrajectoryProcessorId;
    }

    public void setPedestrianTrajectoryProcessorId(int pedestrianTrajectoryProcessorId) {
        this.pedestrianTrajectoryProcessorId = pedestrianTrajectoryProcessorId;
    }

    public double getT() {
        return T;
    }

    public void setT(double t) {
        T = t;
    }

    public double getCellSize() {
        return cellSize;
    }

    public void setCellSize(double cellSize) {
        checkSealed();
        this.cellSize = cellSize;
    }
}
