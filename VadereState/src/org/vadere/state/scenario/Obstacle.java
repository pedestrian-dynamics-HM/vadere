package org.vadere.state.scenario;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VShape;

public class Obstacle implements ScenarioElement {

	private final AttributesObstacle attributes;

	public Obstacle(AttributesObstacle attributes) {
		if (attributes == null)
			throw new IllegalArgumentException("Attributes must not be null.");

		this.attributes = attributes;
	}

	/**
	 * Returns a copy of this obstacle with the same attributes.
	 */
	@Override
	public Obstacle clone() {
		return new Obstacle(attributes);
	}

	@Override
	public VShape getShape() {
		return attributes.getShape();
	}

	@Override
	public int getId() {
		return attributes.getId();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
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
		if (!(obj instanceof Obstacle)) {
			return false;
		}
		Obstacle other = (Obstacle) obj;
		if (attributes == null) {
			if (other.attributes != null) {
				return false;
			}
		} else if (!attributes.equals(other.attributes)) {
			return false;
		}
		return true;
	}

	@Override
	public ScenarioElementType getType() {
		return ScenarioElementType.OBSTACLE;
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}
}
