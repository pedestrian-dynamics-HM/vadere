package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processor.AttributesAreaDensityVoronoiProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;

/**
 * @author Mario Teixeira Parente
 *
 */
@DataProcessorClass(label = "AreaDensityVoronoiProcessor")
public class AreaDensityVoronoiProcessor extends AreaDensityProcessor {

    public AreaDensityVoronoiProcessor(){
        super();
        setAttributes(new AttributesAreaDensityVoronoiProcessor());
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);

        AttributesAreaDensityVoronoiProcessor att = (AttributesAreaDensityVoronoiProcessor) this.getAttributes();
        this.setAlgorithm(new AreaDensityVoronoiAlgorithm(this.getMeasurementArea(), att.getVoronoiArea()));
    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesAreaDensityVoronoiProcessor());
        }

        return super.getAttributes();
    }
}
