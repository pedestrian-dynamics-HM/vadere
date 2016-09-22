package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processor.AttributesPedestrianDensityCountingProcessor;

/**
 * @author Mario Teixeira Parente
 *
 */

public class PedestrianDensityCountingProcessor extends PedestrianDensityProcessor {

	@Override
	public void init(final ProcessorManager manager) {
		AttributesPedestrianDensityCountingProcessor attDensCountProc =
				(AttributesPedestrianDensityCountingProcessor) this.getAttributes();
		this.setAlgorithm(new PointDensityCountingAlgorithm(attDensCountProc.getRadius()));

		super.init(manager);
	}
}
