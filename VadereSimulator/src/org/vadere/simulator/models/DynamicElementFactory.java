package org.vadere.simulator.models;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

public interface DynamicElementFactory {

	/**
	 * Additionally functions as an Abstract Factory for dynamic elements.
	 * 
	 * Note: Every attribute of the given element should be cloned for each individual in this
	 * method, because some fields are individual.
	 */
	<T extends DynamicElement> DynamicElement createElement(VPoint position, int id, Class<T> type);

	/**
	 * Returns the shape which represents the (free) place required by each element.
	 *
	 * @param position the position of the shape i.e. the next created {@link DynamicElement}
	 * @return the shape which represents the (free) place required by each element.
	 */
	VShape getDynamicElementRequiredPlace(@NotNull final VPoint position);
}
