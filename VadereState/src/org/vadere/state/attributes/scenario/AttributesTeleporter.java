package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.Attributes;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((position == null) ? 0 : position.hashCode());
		result = prime * result + ((shift == null) ? 0 : shift.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof AttributesTeleporter)) {
			return false;
		}
		AttributesTeleporter other = (AttributesTeleporter) obj;
		if (position == null) {
			if (other.position != null) {
				return false;
			}
		} else if (!position.equals(other.position)) {
			return false;
		}
		if (shift == null) {
			if (other.shift != null) {
				return false;
			}
		} else if (!shift.equals(other.shift)) {
			return false;
		}
		return true;
	}

}
