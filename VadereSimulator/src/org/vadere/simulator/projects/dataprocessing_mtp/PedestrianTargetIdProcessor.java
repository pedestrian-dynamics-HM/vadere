package org.vadere.simulator.projects.dataprocessing_mtp;

import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.simulator.control.SimulationState;
import org.vadere.state.scenario.Pedestrian;

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
	void init(final AttributesProcessor attributes, final ProcessorFactory factory) {
		// TODO [priority=medium] [task=check] is initialization needed?
	}
}
