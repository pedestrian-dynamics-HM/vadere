package org.vadere.state.scenario;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.geometry.shapes.VShape;

public abstract class ScenarioElement {

	public abstract VShape getShape();
	
	public abstract void setShape(VShape newShape);

	public abstract int getId();

	public abstract ScenarioElementType getType();

	/**
	 * Redeclare the clone method as public to enable copy & paste of scenario
	 * elements in scenario editor.
	 * Subclasses must implement this method using a copy constructor.
	 */
	@Override
	public abstract ScenarioElement clone();

	public abstract Attributes getAttributes();

	public abstract void setAttributes(Attributes attributes);

}
