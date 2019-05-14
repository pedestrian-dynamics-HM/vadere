package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processor.AttributesPedestrianDensityCountingProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

/**
 * @author Mario Teixeira Parente
 * This processor counts the density for each pedestrian (density per pedestrian id) by counting pedestrians that are
 * in 'radius'. To count pedestrians in a measurement area look at AreaDensityCountingProcessor.
 */
@DataProcessorClass()
public class PedestrianDensityCountingProcessor extends PedestrianDensityProcessor {

	public PedestrianDensityCountingProcessor(){
		super();
		setAttributes(new AttributesPedestrianDensityCountingProcessor());
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesPedestrianDensityCountingProcessor attDensCountProc =
				(AttributesPedestrianDensityCountingProcessor) this.getAttributes();
		this.setAlgorithm(new PointDensityCountingAlgorithm(attDensCountProc.getRadius()));

	}

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null || !(super.getAttributes() instanceof AttributesPedestrianDensityCountingProcessor)) {
            setAttributes(new AttributesPedestrianDensityCountingProcessor());
        }

        return super.getAttributes();
    }
}
