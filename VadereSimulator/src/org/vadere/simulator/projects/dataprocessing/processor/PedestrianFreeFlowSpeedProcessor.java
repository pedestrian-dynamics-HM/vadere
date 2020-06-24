package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;


import java.util.OptionalDouble;
import java.util.Set;

/**
 * @author Marion Gödel
 * Save free-flow speeds for each pedestrian in the simulation to evaluate how well the sample fits the defined distribution.
 * In the entry (pedestrianIdKey) "-1" the average is saved
 * In the entry (pedestrianIdKey) "-2" the std of the samples is saved
 */

@DataProcessorClass()
public class PedestrianFreeFlowSpeedProcessor extends DataProcessor<PedestrianIdKey, Double> {

	public PedestrianFreeFlowSpeedProcessor() {
		super("freeFlowSpeed");
	}

	@Override
	protected void doUpdate(final SimulationState state) {

		state.getTopography().getElements(Pedestrian.class)
				.forEach(ped -> this.update(new PedestrianIdKey(ped.getId()), ped.getFreeFlowSpeed()));

	}

	private void update(PedestrianIdKey pedIdKey, double freeFlowSpeed) {
		Set<PedestrianIdKey> keys = this.getKeys();

		if (!keys.contains(pedIdKey))
			this.putValue(pedIdKey, freeFlowSpeed);
	}


	@Override
	public void postLoop(final SimulationState state) {
		// save average in -1 entry
		OptionalDouble meanVal = this.getValues().stream().mapToDouble(a -> a).average();
		if (meanVal.isPresent()){
			this.putValue(new PedestrianIdKey(-1), meanVal.getAsDouble());
		}

		double tmp_sum = this.getValues().stream().mapToDouble(speed -> Math.pow(speed - meanVal.getAsDouble(),2)).sum();
		double std = Math.sqrt(tmp_sum / (this.getValues().size()-1));
		this.putValue(new PedestrianIdKey(-2), std);
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
	}



}
