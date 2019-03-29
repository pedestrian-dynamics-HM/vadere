package org.vadere.state.attributes.processor;

public class AttributesFundamentalDiagramDProcessor extends AttributesAreaProcessor {
	private int measurementAreaId;
	private int voronoiMeasurementAreaId;
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

	public int getVoronoiMeasurementAreaId() {
		return voronoiMeasurementAreaId;
	}

	public void setVoroniMeasurementAreaIdArea(int voronoiMeasurementAreaId) {
		checkSealed();
		this.voronoiMeasurementAreaId = voronoiMeasurementAreaId;
	}

	public void setMeasurementAreaId(int measurementAreaId) {
		checkSealed();
		this.measurementAreaId = measurementAreaId;
	}
}
