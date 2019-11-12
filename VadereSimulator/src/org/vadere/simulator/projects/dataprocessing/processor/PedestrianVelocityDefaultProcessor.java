
package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;

/**
 * This processor computes the velocity based on pedestrians simulation velocity. So the
 * velocity depends on the model implementation and how the velocity of an agent is updated.
 *
 * @author Benedikt Zoennchen
 *
 */
@DataProcessorClass()
public class PedestrianVelocityDefaultProcessor extends APedestrianVelocityProcessor {

	@Override
	protected void doUpdate(SimulationState state) {
		state.getTopography().getPedestrianDynamicElements().getElements()
				.stream()
				.forEach(p -> putValue(new TimestepPedestrianIdKey(state.getStep(),p.getId()), p.getVelocity().getLength()));
	}

	@Override
	public Double getValue(TimestepPedestrianIdKey key) {
		Double velocity = super.getValue(key);
		if(velocity == null) {
			velocity = 0.0;
		}
		return velocity;
	}
}
