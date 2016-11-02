package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;

import java.util.Set;

/**
 * @author Mario Teixeira Parente
 *
 */

public class PedestrianStartTimeProcessor extends DataProcessor<PedestrianIdKey, Double> {

	public PedestrianStartTimeProcessor() {
		super("startTime");
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		state.getTopography().getElements(Pedestrian.class).stream()
				.forEach(ped -> this.update(new PedestrianIdKey(ped.getId()), state.getSimTimeInSec()));
	}

	@Override
	public void init(final ProcessorManager manager) {
		// No initialization needed
	}

	private void update(PedestrianIdKey pedIdKey, double startTime) {
		Set<PedestrianIdKey> keys = this.getKeys();

		if (!keys.contains(pedIdKey))
			this.putValue(pedIdKey, startTime);
	}
}
