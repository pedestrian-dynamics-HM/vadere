package org.vadere.simulator.projects.dataprocessing;

import org.vadere.state.attributes.processors.AttributesAreaProcessor;
import org.vadere.state.attributes.processors.AttributesProcessor;
import org.vadere.util.geometry.shapes.VRectangle;

public abstract class AreaProcessor<V> extends Processor<TimestepDataKey, V> {
    private VRectangle measurementArea;

    @Override
    public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
        AttributesAreaProcessor att = (AttributesAreaProcessor) attributes;
        this.measurementArea = att.getMeasurementArea();
    }

    public VRectangle getMeasurementArea() {
        return this.measurementArea;
    }
}
