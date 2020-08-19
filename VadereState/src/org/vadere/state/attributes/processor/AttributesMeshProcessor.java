package org.vadere.state.attributes.processor;

public class AttributesMeshProcessor extends AttributesProcessor {
	private int measurementAreaId = -1;
	private double edgeLength = 1.0;
	private boolean displayMesh = true;

	public int getMeasurementAreaId() {
		return this.measurementAreaId;
	}

	public void setMeasurementAreaId(final int measurementAreaId) {
		checkSealed();
		this.measurementAreaId = measurementAreaId;
	}

	public double getEdgeLength() {
		return edgeLength;
	}

	public void setEdgeLength(final double edgeLength) {
		checkSealed();
		this.edgeLength = edgeLength;
	}

	public boolean isDisplayMesh() {
		return displayMesh;
	}

	public void setDisplayMesh(boolean displayMesh) {
		checkSealed();
		this.displayMesh = displayMesh;
	}
}
