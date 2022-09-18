package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.Attributes;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

public class AttributesTeleporter extends Attributes {
	private Vector2D shift = new Vector2D(0, 0);
	private VPoint position = new VPoint(0, 0);

	public AttributesTeleporter() {}

	// Getters...

	public Vector2D getTeleporterShift() {
		return shift;
	}

	public VPoint getTeleporterPosition() {
		return position;
	}

}
