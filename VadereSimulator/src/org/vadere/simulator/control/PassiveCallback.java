package org.vadere.simulator.control;

import org.vadere.simulator.models.potential.fields.PotentialFieldTarget;
import org.vadere.state.scenario.Topography;

/**
 * This interface defines a callbacks for the simulation loop.
 * It is called "passive" since it's implementations cannot change the state.
 *
 *
 */
public interface PassiveCallback {
	void preLoop(double simTimeInSec);

	void postLoop(double simTimeInSec);

	void preUpdate(double simTimeInSec);

	void postUpdate(double simTimeInSec);

	void setTopography(Topography scenario);

	default void setPotentialFieldTarget(PotentialFieldTarget potentialFieldTarget){}
}
