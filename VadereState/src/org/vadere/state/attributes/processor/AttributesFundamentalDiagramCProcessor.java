package org.vadere.state.attributes.processor;

public class AttributesFundamentalDiagramCProcessor extends AttributesAreaProcessor {
	private int measurementAreaId;
	private int pedestrianVelocityProcessorId;

	public int getPedestrianVelocityProcessorId() {
		return pedestrianVelocityProcessorId;
	}

	public void setPedestrianVelocityProcessorId(int pedestrianVelocityProcessorId) {
		checkSealed();
		this.pedestrianVelocityProcessorId = pedestrianVelocityProcessorId;
	}

	public int getMeasurementAreaId() {
		return measurementAreaId;
	}

	public void setMeasurementAreaId(int measurementAreaId) {
		checkSealed();
		this.measurementAreaId= measurementAreaId;
	}
}
