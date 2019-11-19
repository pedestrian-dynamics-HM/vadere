package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.models.bhm.PedestrianBHM;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

/**
 * @author Marion Gödel
 *
 */
@DataProcessorClass()
public class PedestrianBehaviourProcessor extends DataProcessor<TimestepPedestrianIdKey, Integer> {

	public PedestrianBehaviourProcessor() {
		super("behaviourId");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		peds.stream().filter(pedestrian -> pedestrian instanceof PedestrianBHM).forEach(p -> this.putValue(new TimestepPedestrianIdKey(state.getStep(), p.getId()),
				((PedestrianBHM) p).getBehaviour()));
	}

}
