package org.vadere.state.attributes.scenario.builder;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesStairs;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.geometry.shapes.VShape;

public final class AttributesStairsBuilder {
	private VShape shape = null;
	private int id = Attributes.ID_NOT_SET;
	private int treadCount = 1;
	private Vector2D upwardDirection = new Vector2D(1.0, 0.0);

	private AttributesStairsBuilder() {
	}

	public static AttributesStairsBuilder anAttributesStairs() {
		return new AttributesStairsBuilder();
	}

	public AttributesStairsBuilder shape(VShape shape) {
		this.shape = shape;
		return this;
	}

	public AttributesStairsBuilder id(int id) {
		this.id = id;
		return this;
	}

	public AttributesStairsBuilder treadCount(int treadCount) {
		this.treadCount = treadCount;
		return this;
	}

	public AttributesStairsBuilder upwardDirection(Vector2D upwardDirection) {
		this.upwardDirection = upwardDirection;
		return this;
	}

	public AttributesStairs build() {
		return new AttributesStairs(id, shape, treadCount, upwardDirection);
	}
}
