package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.attributes.processor.AttributesAreaProcessor;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.util.geometry.shapes.VRectangle;

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
        this.measurementArea = manager.getMeasurementArea(att.getMeasurementAreaId());
        if (measurementArea == null)
            throw new RuntimeException(String.format("MeasurementArea with index %d does not exist.", att.getMeasurementAreaId()));
        if (!measurementArea.isRectangular())
            throw new RuntimeException("DataProcessor and IntegralVoronoiAlgorithm only supports Rectangular measurement areas.");

    }

    public MeasurementArea getMeasurementArea() {
        return this.measurementArea;
    }
}
