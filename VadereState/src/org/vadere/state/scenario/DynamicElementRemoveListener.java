package org.vadere.state.scenario;

/**
 * Called when dynamic elements are removed from the dynamic element container.
 */
public interface DynamicElementRemoveListener<T extends DynamicElement> {
	void elementRemoved(T element);
}
