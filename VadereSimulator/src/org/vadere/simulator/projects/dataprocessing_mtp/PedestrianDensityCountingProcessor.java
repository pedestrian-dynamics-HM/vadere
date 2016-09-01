package org.vadere.simulator.projects.dataprocessing_mtp;

public class PedestrianDensityCountingProcessor extends PedestrianDensityProcessor {

	@Override
	public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
		AttributesPedestrianDensityCountingProcessor attDensCountProc =
				(AttributesPedestrianDensityCountingProcessor) attributes;
		this.setAlgorithm(new PointDensityCountingAlgorithm(attDensCountProc.getRadius()));

		super.init(attributes, manager);
	}
}
