package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Mario Teixeira Parente
 */

public class PedestrianOSMStrideLengthProcessor extends DataProcessor<TimestepPedestrianIdKey, Double> {
	private OptimalStepsModel osm;

	public PedestrianOSMStrideLengthProcessor() {
		super("strideLength");

		this.osm = null;
	}

	@Override
	protected void doUpdate(final SimulationState state) {

		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);
		if (osm != null) {
			List<PedestrianOSM> osmPeds = peds.stream().map(p -> ((PedestrianOSM) p)).collect(Collectors.toList());
			osmPeds.forEach(ped -> {

				LinkedList<Double> strideLengths = ped.getStrides()[0];
				double length = strideLengths.isEmpty() ? 0.0 : strideLengths.getLast();

				this.putValue(new TimestepPedestrianIdKey(state.getStep(), ped.getId()), length);
			});
		} else {
			peds.forEach(ped -> this.putValue(new TimestepPedestrianIdKey(state.getStep(), ped.getId()), Double.NaN));
		}
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		Model model = manager.getMainModel();
		if (model instanceof OptimalStepsModel)
			this.osm = (OptimalStepsModel) model;
	}
}
