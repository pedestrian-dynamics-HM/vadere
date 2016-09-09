package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processor.AttributesAreaDensityVoronoiProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

public class AreaDensityVoronoiProcessor extends AreaDensityProcessor {
    @Override
    public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
        super.init(attributes, manager);

        AttributesAreaDensityVoronoiProcessor att = (AttributesAreaDensityVoronoiProcessor) attributes;
        this.setAlgorithm(new AreaDensityVoronoiAlgorithm(this.getMeasurementArea(), att.getVoronoiArea()));
    }
}
