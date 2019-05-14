package org.vadere.state.attributes.processor;


public class AttributesFundamentalDiagramBProcessor extends AttributesProcessor {
	private int pedestrianTrajectoryProcessorId;
	private int measurementAreaId;

	public int getPedestrianTrajectoryProcessorId() {
		return pedestrianTrajectoryProcessorId;
	}

	public int getMeasurementAreaId() {
		return measurementAreaId;
	}

	public void setMeasurementAreaId(int measurementAreaId) {
		checkSealed();
		this.measurementAreaId = measurementAreaId;
	}

	public void setPedestrianTrajectoryProcessorId(int pedestrianTrajectoryProcessorId) {
		checkSealed();
		this.pedestrianTrajectoryProcessorId = pedestrianTrajectoryProcessorId;
	}
}
