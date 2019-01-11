package org.vadere.state.attributes.processor;

import org.vadere.util.geometry.shapes.VRectangle;

public class AttributesFundamentalDiagramEProcessor extends AttributesAreaProcessor {
	private VRectangle measurementArea;
	private VRectangle voronoiArea;
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

	public VRectangle getVoronoiArea() {
		return voronoiArea;
	}

	public void setVoronoiArea(VRectangle voronoiArea) {
		checkSealed();
		this.voronoiArea = voronoiArea;
	}

	public void setMeasurementArea(VRectangle measurementArea) {
		checkSealed();
		this.measurementArea = measurementArea;
	}
}