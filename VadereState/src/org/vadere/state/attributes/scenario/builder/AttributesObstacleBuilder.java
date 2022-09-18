package org.vadere.state.attributes.scenario.builder;

import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.util.geometry.shapes.VShape;

public final class AttributesObstacleBuilder {
	private VShape shape;
	private int id;

	private AttributesVisualElementBuilder visualElementBuilder;

	private AttributesObstacleBuilder() {
	}

	public static AttributesObstacleBuilder anAttributesObstacle() {
		return new AttributesObstacleBuilder();
	}


	public AttributesObstacleBuilder shape(VShape shape) {
		this.shape = shape;
		return this;
	}

	public AttributesObstacleBuilder id(int id) {
		this.id = id;
		return this;
	}


	public AttributesObstacle build() {
		AttributesObstacle attributesObstacle = new AttributesObstacle(id);
		attributesObstacle.setShape(shape);
		return attributesObstacle;
	}

}
