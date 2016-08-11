package org.vadere.state.scenario;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VShape;

public interface ScenarioElement extends Cloneable {
	VShape getShape();

	int getId();

	ScenarioElementType getType();

	ScenarioElement clone();

	Attributes getAttributes();
}
