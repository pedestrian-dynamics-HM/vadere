package org.vadere.simulator.projects.dataprocessing;

public class AttributesAreaSpeedProcessor extends AttributesAreaProcessor {
    private int pedestrianPositionProcessorId;
    private int pedestrianVelocityProcessorId;

    public int getPedestrianPositionProcessorId() {
        return this.pedestrianPositionProcessorId;
    }

    public int getPedestrianVelocityProcessorId() {
        return this.pedestrianVelocityProcessorId;
    }
}
