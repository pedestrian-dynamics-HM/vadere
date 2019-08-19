package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

/**
 * Save most important event of a pedestrian in each time step in an own column.
 */
@DataProcessorClass()
public class PedestrianMostImportantEventProcessor extends DataProcessor<TimestepPedestrianIdKey, String> {

	public PedestrianMostImportantEventProcessor() {
		super("mostImportantEvent");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		Collection<Pedestrian> pedestrians = state.getTopography().getElements(Pedestrian.class);

		pedestrians.forEach(pedestrian -> this.putValue(
				new TimestepPedestrianIdKey(state.getStep(), pedestrian.getId()),
				pedestrian.getMostImportantEvent().toStringForOutputProcessor())
		);
	}

}
