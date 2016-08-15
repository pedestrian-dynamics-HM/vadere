package org.vadere.simulator.projects.dataprocessing_mtp;

public class PedestrianDensityCountingProcessor extends PedestrianDensityProcessor {

	@Override
	void init(final AttributesProcessor attributes, final ProcessorFactory factory) {
		AttributesPedestrianDensityCountingProcessor attDensCountProc =
				(AttributesPedestrianDensityCountingProcessor) attributes;
		this.setAlgorithm(new PointDensityCountingAlgorithm(attDensCountProc.getRadius()));

		super.init(attributes, factory);
	}
}
