package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.util.geometry.shapes.VRectangle;

public abstract class AreaProcessor<V> extends Processor<TimestepDataKey, V> {
    private VRectangle measurementArea;

    @Override
    void init(final AttributesProcessor attributes, final ProcessorFactory factory) {
        AttributesAreaProcessor att = (AttributesAreaProcessor) attributes;
        this.measurementArea = att.getMeasurementArea();
    }

    public VRectangle getMeasurementArea() {
        return this.measurementArea;
    }
}
