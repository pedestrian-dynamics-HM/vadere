package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

/**
 * @author Mario Teixeira Parente
 *
 */

public class PedestrianTargetIdProcessor extends DataProcessor<TimestepPedestrianIdKey, Integer> {

	public PedestrianTargetIdProcessor() {
		super("targetId");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		peds.forEach(p -> this.setValue(new TimestepPedestrianIdKey(state.getStep(), p.getId()),
				p.getTargets().isEmpty() ? -1 : p.getTargets().getFirst()));
	}

	@Override
	public void init(final ProcessorManager manager) {
		// No initialization needed
	}
}
