package org.vadere.state.scenario;

public interface DynamicElementAddListener<T extends DynamicElement> {
	/**
	 * elementAdded() is called when a new element is added to the observer of this listener.
	 * 
	 * @param element The element added to the observer.
	 */
	public void elementAdded(T element);
}
