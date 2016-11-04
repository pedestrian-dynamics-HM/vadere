package org.vadere.state.scenario;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VShape;

public interface ScenarioElement extends Cloneable {

	VShape getShape();
	
	default void setShape(VShape newShape) {
		throw new UnsupportedOperationException("This concrete scenario element does not support setting the shape.");
	}

	int getId();

	ScenarioElementType getType();

	ScenarioElement clone();

	Attributes getAttributes();
}
