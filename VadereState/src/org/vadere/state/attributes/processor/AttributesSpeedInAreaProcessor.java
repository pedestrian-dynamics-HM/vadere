package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 */

public class AttributesSpeedInAreaProcessor extends AttributesProcessor {

    // Variables
	private int measurementAreaId = -1;
    private int pedestrianVelocityProcessorId = -1;

    // Getter
    public int getMeasurementAreaId() {
        return this.measurementAreaId;
    }
    public int getPedestrianVelocityProcessorId() {
        return this.pedestrianVelocityProcessorId;
    }

    // Setter
    public void setMeasurementAreaId(int measurementAreaId) {
        checkSealed();
        this.measurementAreaId = measurementAreaId;
    }

    public void setPedestrianVelocityProcessorId(int pedestrianVelocityProcessorId) {
        checkSealed();
        this.pedestrianVelocityProcessorId = pedestrianVelocityProcessorId;
    }
}
