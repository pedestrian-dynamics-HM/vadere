package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processor.AttributesPedestrianDensityCountingProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

public class PedestrianDensityCountingProcessor extends PedestrianDensityProcessor {

	@Override
	public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
		AttributesPedestrianDensityCountingProcessor attDensCountProc =
				(AttributesPedestrianDensityCountingProcessor) attributes;
		this.setAlgorithm(new PointDensityCountingAlgorithm(attDensCountProc.getRadius()));

		super.init(attributes, manager);
	}
}
