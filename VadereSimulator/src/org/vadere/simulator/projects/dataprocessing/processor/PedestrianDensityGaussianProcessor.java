package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processor.AttributesPedestrianDensityGaussianProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

/**
 * @author Mario Teixeira Parente
 *
 */
@DataProcessorClass()
public class PedestrianDensityGaussianProcessor extends PedestrianDensityProcessor {

	public PedestrianDensityGaussianProcessor () {
		super();
		setAttributes(new AttributesPedestrianDensityGaussianProcessor());
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesPedestrianDensityGaussianProcessor attDensGauss =
				(AttributesPedestrianDensityGaussianProcessor) this.getAttributes();
		this.setAlgorithm(new PointDensityGaussianAlgorithm(attDensGauss.getScale(), attDensGauss.getStandardDeviation(),
				attDensGauss.isObstacleDensity()));

	}

	@Override
	public AttributesProcessor getAttributes() {
		if(super.getAttributes() == null) {
			setAttributes(new AttributesPedestrianDensityGaussianProcessor());
		}

		return super.getAttributes();
	}
}
