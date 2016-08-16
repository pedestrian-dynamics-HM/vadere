package org.vadere.simulator.projects.dataprocessing_mtp;

public class AreaDensityVoronoiProcessor extends AreaDensityProcessor {
    @Override
    void init(final AttributesProcessor attributes, final ProcessorManager factory) {
        super.init(attributes, factory);

        AttributesAreaDensityVoronoiProcessor att = (AttributesAreaDensityVoronoiProcessor) attributes;
        this.setAlgorithm(new AreaDensityVoronoiAlgorithm(this.getMeasurementArea(), att.getVoronoiArea()));
    }
}
