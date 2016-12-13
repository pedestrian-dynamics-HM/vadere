package org.vadere.state.scenario;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VShape;

public abstract class ScenarioElement implements Cloneable {

	public abstract VShape getShape();
	
	public void setShape(VShape newShape) {
		throw new UnsupportedOperationException("This concrete scenario element does not support setting the shape.");
	}

	public abstract int getId();

	public abstract ScenarioElementType getType();

	/**
	 * Redeclare the clone method as public to enable copy & paste of scenario
	 * elements in scenario editor.
	 */
	@Override
	public ScenarioElement clone() {
		try {
			return (ScenarioElement) super.clone();
		} catch (CloneNotSupportedException e) {
			// this case should never happen (because this base class is cloneable)
			// unless a subclass contains not-cloneable fields or so 
			throw new RuntimeException(e);
		}
	}

	public abstract Attributes getAttributes();
}
