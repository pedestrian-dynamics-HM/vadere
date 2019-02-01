package org.vadere.state.attributes.processor;

import org.vadere.util.geometry.shapes.VRectangle;

public class AttributesFundamentalDiagramCProcessor extends AttributesAreaProcessor {
	private VRectangle measurementArea;
	private int pedestrianVelocityProcessorId;

	public int getPedestrianVelocityProcessorId() {
		return pedestrianVelocityProcessorId;
	}

	public void setPedestrianVelocityProcessorId(int pedestrianVelocityProcessorId) {
		checkSealed();
		this.pedestrianVelocityProcessorId = pedestrianVelocityProcessorId;
	}

	public VRectangle getMeasurementArea() {
		return measurementArea;
	}

	public void setMeasurementArea(VRectangle measurementArea) {
		checkSealed();
		this.measurementArea = measurementArea;
	}
}
