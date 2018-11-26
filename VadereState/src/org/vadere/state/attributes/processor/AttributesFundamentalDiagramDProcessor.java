package org.vadere.state.attributes.processor;

import org.vadere.util.geometry.shapes.VRectangle;

public class AttributesFundamentalDiagramDProcessor extends AttributesAreaProcessor {
	private int pedestrianTrajectoryProcessorId;
	private VRectangle measurementArea;

	public int getPedestrianTrajectoryProcessorId() {
		return pedestrianTrajectoryProcessorId;
	}

	public VRectangle getMeasurementArea() {
		return measurementArea;
	}

	public void setMeasurementArea(VRectangle measurementArea) {
		checkSealed();
		this.measurementArea = measurementArea;
	}

	public void setPedestrianTrajectoryProcessorId(int pedestrianTrajectoryProcessorId) {
		checkSealed();
		this.pedestrianTrajectoryProcessorId = pedestrianTrajectoryProcessorId;
	}
}
