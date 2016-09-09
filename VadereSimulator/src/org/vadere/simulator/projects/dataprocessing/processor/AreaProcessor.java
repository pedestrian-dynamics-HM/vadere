package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepDataKey;
import org.vadere.state.attributes.processor.AttributesAreaProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
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
