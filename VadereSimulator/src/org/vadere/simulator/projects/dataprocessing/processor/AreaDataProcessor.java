package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.attributes.processor.AttributesAreaProcessor;
import org.vadere.state.scenario.MeasurementArea;

/**
 * @author Mario Teixeira Parente
 *
 */

public abstract class AreaDataProcessor<V> extends DataProcessor<TimestepKey, V> {
    private MeasurementArea measurementArea;

    protected AreaDataProcessor(final String... headers) {
        super(headers);
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesAreaProcessor att = (AttributesAreaProcessor) this.getAttributes();
        this.measurementArea = manager.getMeasurementArea(att.getMeasurementAreaId(), false);
    }

    public MeasurementArea getMeasurementArea() {
        return this.measurementArea;
    }
}
