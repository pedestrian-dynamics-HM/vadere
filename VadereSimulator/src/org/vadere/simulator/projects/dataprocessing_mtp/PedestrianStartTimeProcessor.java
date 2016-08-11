package org.vadere.simulator.projects.dataprocessing_mtp;

import java.util.Set;

import org.vadere.simulator.control.SimulationState;
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
	void init(final AttributesProcessor attributes, final ProcessorFactory factory) {
		// TODO [priority=medium] [task=check] is initialization needed?
	}

	private void update(PedestrianIdDataKey pedIdKey, double startTime) {
		Set<PedestrianIdDataKey> keys = this.getKeys();

		if (!keys.contains(pedIdKey))
			this.setValue(pedIdKey, startTime);
	}
}
