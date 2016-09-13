package org.vadere.simulator.control;


/**
 * This interface defines a callbacks for the simulation loop.
 * It's implementations define the major part of the simulation model's logic.
 * It is called "active" since it's implementations do change the state.
 *
 *
 */
public interface ActiveCallback {

	void preLoop(final double simTimeInSec);

	void postLoop(final double simTimeInSec);

	void update(final double simTimeInSec);
}
