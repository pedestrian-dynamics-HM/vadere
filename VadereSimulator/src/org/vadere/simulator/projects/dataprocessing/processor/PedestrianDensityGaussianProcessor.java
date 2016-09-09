package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processor.AttributesPedestrianDensityGaussianProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

public class PedestrianDensityGaussianProcessor extends PedestrianDensityProcessor {

	@Override
	public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
		AttributesPedestrianDensityGaussianProcessor attDensGauss =
				(AttributesPedestrianDensityGaussianProcessor) attributes;
		this.setAlgorithm(new PointDensityGaussianAlgorithm(attDensGauss.getScale(), attDensGauss.getStandardDerivation(),
				attDensGauss.isObstacleDensity()));

		super.init(attributes, manager);
	}
}
