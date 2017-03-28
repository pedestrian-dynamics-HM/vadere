package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processor.AttributesPedestrianDensityGaussianProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

/**
 * @author Mario Teixeira Parente
 *
 */

public class PedestrianDensityGaussianProcessor extends PedestrianDensityProcessor {

	@Override
	public void init(final ProcessorManager manager) {
		AttributesPedestrianDensityGaussianProcessor attDensGauss =
				(AttributesPedestrianDensityGaussianProcessor) this.getAttributes();
		this.setAlgorithm(new PointDensityGaussianAlgorithm(attDensGauss.getScale(), attDensGauss.getStandardDerivation(),
				attDensGauss.isObstacleDensity()));

		super.init(manager);
	}

	@Override
	public AttributesProcessor getAttributes() {
		if(super.getAttributes() == null) {
			setAttributes(new AttributesPedestrianDensityGaussianProcessor());
		}

		return super.getAttributes();
	}
}
