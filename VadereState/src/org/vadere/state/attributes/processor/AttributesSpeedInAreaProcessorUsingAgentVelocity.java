package org.vadere.state.attributes.processor;

public class AttributesSpeedInAreaProcessorUsingAgentVelocity extends AttributesProcessor {

    // Variables
	private int measurementAreaId = -1;
    private int pedestrianVelocityDefaultProcessor = -1;

    // Getter
    public int getMeasurementAreaId() {
        return this.measurementAreaId;
    }
    public int getPedestrianVelocityDefaultProcessorId() {
        return this.pedestrianVelocityDefaultProcessor;
    }

    // Setter
    public void setMeasurementAreaId(int measurementAreaId) {
        checkSealed();
        this.measurementAreaId = measurementAreaId;
    }

    public void setPedestrianVelocityDefaultProcessorId(int pedestrianVelocityDefaultProcessor) {
        checkSealed();
        this.pedestrianVelocityDefaultProcessor = pedestrianVelocityDefaultProcessor;
    }

}
