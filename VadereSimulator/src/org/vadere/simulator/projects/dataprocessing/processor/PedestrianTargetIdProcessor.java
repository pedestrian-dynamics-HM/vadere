package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

/**
 * @author Mario Teixeira Parente
 *
 */
@DataProcessorClass()
public class PedestrianTargetIdProcessor extends DataProcessor<TimestepPedestrianIdKey, Integer> {

	public PedestrianTargetIdProcessor() {
		super("targetId");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		peds.forEach(p -> this.putValue(new TimestepPedestrianIdKey(state.getStep(), p.getId()),
				!p.hasNextTarget() ? -1 :
						p.getNextTargetId()));
	}

}
