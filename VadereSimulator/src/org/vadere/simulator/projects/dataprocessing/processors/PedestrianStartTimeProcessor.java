package org.vadere.simulator.projects.dataprocessing.processors;

import java.util.Set;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakeys.PedestrianIdDataKey;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processors.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;

public class PedestrianStartTimeProcessor extends Processor<PedestrianIdDataKey, Double> {

	public PedestrianStartTimeProcessor() {
		super("tstart");
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		state.getTopography().getElements(Pedestrian.class).stream()
				.forEach(ped -> this.update(new PedestrianIdDataKey(ped.getId()), state.getSimTimeInSec()));
	}

	@Override
	public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
		// No initialization needed
	}

	private void update(PedestrianIdDataKey pedIdKey, double startTime) {
		Set<PedestrianIdDataKey> keys = this.getKeys();

		if (!keys.contains(pedIdKey))
			this.addValue(pedIdKey, startTime);
	}
}
