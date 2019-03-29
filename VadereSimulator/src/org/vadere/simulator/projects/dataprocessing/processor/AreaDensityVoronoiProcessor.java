package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processor.AttributesAreaDensityVoronoiProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.util.factory.processors.ProcessorFlag;

import java.util.List;

/**
 * @author Mario Teixeira Parente
 *
 */
@DataProcessorClass(label = "AreaDensityVoronoiProcessor", processorFlags = {ProcessorFlag.needMeasurementArea})
public class AreaDensityVoronoiProcessor extends AreaDensityProcessor {

    public AreaDensityVoronoiProcessor(){
        super();
        setAttributes(new AttributesAreaDensityVoronoiProcessor());
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);

        AttributesAreaDensityVoronoiProcessor att = (AttributesAreaDensityVoronoiProcessor) this.getAttributes();
        MeasurementArea measurementArea = manager.getMeasurementArea(att.getMeasurementAreaId());
        if (measurementArea == null)
            throw new RuntimeException(String.format("MeasurementArea with index %d does not exist.", att.getMeasurementAreaId()));
        if (!measurementArea.isRectangular())
            throw new RuntimeException("DataProcessor and IntegralVoronoiAlgorithm only supports Rectangular measurement areas.");

        this.setAlgorithm(new AreaDensityVoronoiAlgorithm(this.getMeasurementArea(), measurementArea));
    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesAreaDensityVoronoiProcessor());
        }

        return super.getAttributes();
    }

    @Override
    public boolean sanityCheck(Object o) {
        List<MeasurementArea> data = (List<MeasurementArea>) o;
        AttributesAreaDensityVoronoiProcessor att = (AttributesAreaDensityVoronoiProcessor) this.getAttributes();

        boolean match1 = data.stream().map(MeasurementArea::getId).anyMatch(id -> id == att.getVoronoiMeasurementAreaId());
        boolean match2 = data.stream().map(MeasurementArea::getId).anyMatch(id -> id == att.getMeasurementAreaId());

        return match1 && match2;
    }
}
