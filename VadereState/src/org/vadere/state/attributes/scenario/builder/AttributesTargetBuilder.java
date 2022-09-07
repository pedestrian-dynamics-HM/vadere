package org.vadere.state.attributes.scenario.builder;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.util.geometry.shapes.VShape;

public final class AttributesTargetBuilder {
	private int id = Attributes.ID_NOT_SET;
	private boolean absorbing = true;
	private VShape shape;

	private AttributesTargetBuilder() {
	}

	public static AttributesTargetBuilder anAttributesTarget() {
		return new AttributesTargetBuilder();
	}

	public AttributesTargetBuilder id(int id) {
		this.id = id;
		return this;
	}

	public AttributesTargetBuilder absorbing(boolean absorbing) {
		this.absorbing = absorbing;
		return this;
	}

	public AttributesTargetBuilder shape(VShape shape) {
		this.shape = shape;
		return this;
	}

	public AttributesTarget build() {
		return new AttributesTarget(shape, id);
	}
}
