package org.vadere.simulator.projects.dataprocessing.processor;

import java.util.Collection;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdDataKey;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;

public class PedestrianSourceIdProcessor extends Processor<PedestrianIdDataKey, Integer> {

	public PedestrianSourceIdProcessor() {
		super("sid");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		peds.forEach(p -> this.addValue(new PedestrianIdDataKey(p.getId()), -1));
	}

	@Override
	public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
		// No initialization needed
	}
}
