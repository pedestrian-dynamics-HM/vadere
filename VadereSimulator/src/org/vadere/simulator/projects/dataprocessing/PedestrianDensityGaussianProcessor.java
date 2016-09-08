package org.vadere.simulator.projects.dataprocessing;

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
