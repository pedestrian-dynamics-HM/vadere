package org.vadere.state.scenario;

/**
 * Called when dynamic elements are removed from the dynamic element container.
 */
public interface DynamicElementRemoveListener<T extends DynamicElement> {
	public void elementRemoved(T element);
}
