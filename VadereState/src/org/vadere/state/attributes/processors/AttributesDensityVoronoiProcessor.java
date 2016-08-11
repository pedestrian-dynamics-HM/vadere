package org.vadere.state.attributes.processors;

import org.vadere.state.attributes.Attributes;
import org.vadere.util.geometry.shapes.VRectangle;

public class AttributesDensityVoronoiProcessor extends Attributes {

	private VRectangle measurementArea = new VRectangle(0, 0, 1, 1);

	private VRectangle voronoiArea = new VRectangle(0, 0, 1, 1);

	public VRectangle getMeasurementArea() {
		return measurementArea;
	}

	public VRectangle getVoronoiArea() {
		return voronoiArea;
	}
}
