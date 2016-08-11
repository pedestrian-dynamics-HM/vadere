package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.state.attributes.processors.AttributesDensityVoronoiProcessor;

public class AreaDensityVoronoiProcessor extends AreaDensityProcessor {
    @Override
    void init(final AttributesProcessor attributes, final ProcessorFactory factory) {
        super.init(attributes, factory);

        AttributesAreaDensityVoronoiProcessor att = (AttributesAreaDensityVoronoiProcessor) attributes;
        this.setAlgorithm(new AreaDensityVoronoiAlgorithm(this.getMeasurementArea(), att.getVoronoiArea()));
    }
}
