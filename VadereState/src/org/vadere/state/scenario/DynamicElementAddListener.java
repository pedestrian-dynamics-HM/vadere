package org.vadere.state.scenario;

/**
 * Called when dynamic elements are added to the dynamic element container.
 */
public interface DynamicElementAddListener<T extends DynamicElement> {
	void elementAdded(T element);
}
