package org.vadere.state.attributes.processor;


public class AttributesFundamentalDiagramBProcessor extends AttributesProcessor {
	private int pedestrianTrajectoryProcessorId;
	private int measurementArea;

	public int getPedestrianTrajectoryProcessorId() {
		return pedestrianTrajectoryProcessorId;
	}

	public int getMeasurementArea() {
		return measurementArea;
	}

	public void setMeasurementArea(int measurementArea) {
		checkSealed();
		this.measurementArea = measurementArea;
	}

	public void setPedestrianTrajectoryProcessorId(int pedestrianTrajectoryProcessorId) {
		checkSealed();
		this.pedestrianTrajectoryProcessorId = pedestrianTrajectoryProcessorId;
	}
}
