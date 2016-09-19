package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdDataKey;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;

import java.util.Collection;

public class PedestrianSourceIdProcessor extends DataProcessor<PedestrianIdDataKey, Integer> {

	public PedestrianSourceIdProcessor() {
		super("sid");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		peds.forEach(p -> {
			final Source s = p.getSource();
			final int sourceId = (s == null) ? -1 : s.getId();
			this.addValue(new PedestrianIdDataKey(p.getId()), sourceId);
		});
	}

	@Override
	public void init(final ProcessorManager manager) {
		// No initialization needed
	}
}
