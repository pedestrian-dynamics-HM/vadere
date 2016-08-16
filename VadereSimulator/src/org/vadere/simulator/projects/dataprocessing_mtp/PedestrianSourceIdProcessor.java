package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

public class PedestrianSourceIdProcessor extends Processor<PedestrianIdDataKey, Integer> {

	public PedestrianSourceIdProcessor() {
		super("sid");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		peds.stream().forEach(p -> this.setValue(new PedestrianIdDataKey(p.getId()), -1));
	}

	@Override
	void init(final AttributesProcessor attributes, final ProcessorManager factory) {
		// No initialization needed
	}
}
