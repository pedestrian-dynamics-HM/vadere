package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

public class PedestrianTargetIdProcessor extends Processor<TimestepPedestrianIdDataKey, Integer> {

	public PedestrianTargetIdProcessor() {
		super("tid");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		peds.stream().forEach(p -> this.setValue(new TimestepPedestrianIdDataKey(state.getStep(), p.getId()),
				p.getTargets().isEmpty() ? -1 : p.getTargets().getFirst()));
	}

	@Override
	void init(final AttributesProcessor attributes, final ProcessorManager manager) {
		// No initialization needed
	}
}
