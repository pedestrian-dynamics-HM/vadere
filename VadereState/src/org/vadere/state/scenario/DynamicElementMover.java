package org.vadere.state.scenario;

import org.vadere.util.geometry.shapes.VPoint;

public interface DynamicElementMover {

	/**
	 * @param element		the DynamicElement which was moved to a new position
	 * @param oldPosition	the old potion the DynamicElement was prior to this call. (The new position is contained in the element)
	 */
	<T extends DynamicElement> void moveElement(T element, final VPoint oldPosition);

	<T extends DynamicElement> void addElement(T element);

	<T extends DynamicElement> void removeElement(T element);

}
