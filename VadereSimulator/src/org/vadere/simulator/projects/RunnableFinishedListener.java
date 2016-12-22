package org.vadere.simulator.projects;

/**
 * Used to notify the caller when a Runnable finishes its {@code run} method.
 * This is useful when we want to want to do something when a thread finishes.
 * 
 * @author Jakob Schöttl
 *
 */
public interface RunnableFinishedListener {
	void finished(Runnable runnable);
}
