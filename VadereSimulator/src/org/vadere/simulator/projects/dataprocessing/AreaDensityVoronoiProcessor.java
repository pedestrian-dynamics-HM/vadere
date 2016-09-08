package org.vadere.simulator.projects.dataprocessing;

import org.vadere.state.attributes.processors.AttributesAreaDensityVoronoiProcessor;
import org.vadere.state.attributes.processors.AttributesProcessor;

public class AreaDensityVoronoiProcessor extends AreaDensityProcessor {
    @Override
    public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
        super.init(attributes, manager);

        AttributesAreaDensityVoronoiProcessor att = (AttributesAreaDensityVoronoiProcessor) attributes;
        this.setAlgorithm(new AreaDensityVoronoiAlgorithm(this.getMeasurementArea(), att.getVoronoiArea()));
    }
}
