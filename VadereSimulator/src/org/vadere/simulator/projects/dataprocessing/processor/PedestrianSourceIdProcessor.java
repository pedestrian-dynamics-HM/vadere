package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;

import java.util.Collection;

/**
 * @author Mario Teixeira Parente
 *
 */
@DataProcessorClass()
public class PedestrianSourceIdProcessor extends DataProcessor<PedestrianIdKey, Integer> {

	public PedestrianSourceIdProcessor() {
		super("sourceId");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		peds.forEach(p -> {
			final Source s = p.getSource();
			final int sourceId = (s == null) ? -1 : s.getId();
			this.putValue(new PedestrianIdKey(p.getId()), sourceId);
		});
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
	}
}
