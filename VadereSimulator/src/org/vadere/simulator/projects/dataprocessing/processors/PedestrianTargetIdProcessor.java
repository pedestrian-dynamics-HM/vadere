package org.vadere.simulator.projects.dataprocessing.processors;

import java.util.Collection;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakeys.TimestepPedestrianIdDataKey;
import org.vadere.state.attributes.processors.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;

public class PedestrianTargetIdProcessor extends Processor<TimestepPedestrianIdDataKey, Integer> {

	public PedestrianTargetIdProcessor() {
		super("tid");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		peds.forEach(p -> this.addValue(new TimestepPedestrianIdDataKey(state.getStep(), p.getId()),
				p.getTargets().isEmpty() ? -1 : p.getTargets().getFirst()));
	}

	@Override
	public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
		// No initialization needed
	}
}
