package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianLineCrossProcessor;

public class PedestrianLineCrossProcessor extends DataProcessor<PedestrianIdKey, Double> {

	public PedestrianLineCrossProcessor() {
		super("crossTime");
		setAttributes(new AttributesPedestrianLineCrossProcessor());
	}

	@Override
	protected void doUpdate(SimulationState state) {

	}
}
