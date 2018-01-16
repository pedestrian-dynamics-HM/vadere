package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processor.AttributesPedestrianDensityCountingProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

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

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesPedestrianDensityCountingProcessor());
        }

        return super.getAttributes();
    }
}
