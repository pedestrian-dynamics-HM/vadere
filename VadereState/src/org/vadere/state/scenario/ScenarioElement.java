package org.vadere.state.scenario;

import org.vadere.state.attributes.AttributesScenarioElement;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VShape;

public abstract class ScenarioElement<T extends AttributesScenarioElement> extends AttributesAttached<T> {

	public abstract VShape getShape();

	public boolean overlapWith(ScenarioElement element){
		return getShape().intersects(element.getShape());
	}

	public boolean totalOverlapWith(ScenarioElement element){
		return getShape().sameArea(element.getShape());
	}

	public boolean enclosesScenarioElement(ScenarioElement element){
		return getShape().containsShape(element.getShape());
	}

	public abstract void setShape(VShape newShape);

	public abstract int getId();

	public abstract void setId(int id);

	public abstract ScenarioElementType getType();

	/**
	 * Redeclare the clone method as public to enable copy & paste of scenario
	 * elements in scenario editor.
	 * Subclasses must implement this method using a copy constructor.
	 */
	@Override
	public abstract ScenarioElement clone();
}
