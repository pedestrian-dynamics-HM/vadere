package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;

import java.util.Set;

/**
 * @author Mario Teixeira Parente
 *
 */
@DataProcessorClass()
public class PedestrianStartTimeProcessor extends DataProcessor<PedestrianIdKey, Double> {

	public PedestrianStartTimeProcessor() {
		super("startTime");
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		state.getTopography().getElements(Pedestrian.class)
				.forEach(ped -> this.update(new PedestrianIdKey(ped.getId()), state.getSimTimeInSec()));
	}

	private void update(PedestrianIdKey pedIdKey, double startTime) {
		Set<PedestrianIdKey> keys = this.getKeys();

		if (!keys.contains(pedIdKey))
			this.putValue(pedIdKey, startTime);
	}
}
