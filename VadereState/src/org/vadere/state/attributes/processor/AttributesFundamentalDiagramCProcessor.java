package org.vadere.state.attributes.processor;

import org.vadere.util.geometry.shapes.VRectangle;

public class AttributesFundamentalDiagramCProcessor extends AttributesAreaProcessor {
	private VRectangle measurementArea;

	public VRectangle getMeasurementArea() {
		return measurementArea;
	}

	public void setMeasurementArea(VRectangle measurementArea) {
		checkSealed();
		this.measurementArea = measurementArea;
	}
}
