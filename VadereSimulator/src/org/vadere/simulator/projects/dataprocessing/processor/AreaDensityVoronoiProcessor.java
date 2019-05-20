package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.flags.UsesMeasurementArea;
import org.vadere.state.attributes.processor.AttributesAreaDensityVoronoiProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.util.factory.processors.Flag;

import java.util.List;

/**
 * @author Mario Teixeira Parente
 *
 */
@DataProcessorClass(label = "AreaDensityVoronoiProcessor")
public class AreaDensityVoronoiProcessor extends AreaDensityProcessor implements UsesMeasurementArea {

    public AreaDensityVoronoiProcessor(){
        super();
        setAttributes(new AttributesAreaDensityVoronoiProcessor());
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesAreaDensityVoronoiProcessor att = (AttributesAreaDensityVoronoiProcessor) this.getAttributes();

        MeasurementArea measurementArea = manager.getMeasurementArea(att.getMeasurementAreaId(), true);
        MeasurementArea measurementVoronoiArea = manager.getMeasurementArea(att.getVoronoiMeasurementAreaId(), true);

        this.setAlgorithm(new AreaDensityVoronoiAlgorithm(measurementVoronoiArea, measurementArea));
    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesAreaDensityVoronoiProcessor());
        }

        return super.getAttributes();
    }


    @Override
    public int[] getReferencedMeasurementAreaId() {
        AttributesAreaDensityVoronoiProcessor att = (AttributesAreaDensityVoronoiProcessor) this.getAttributes();
        return new int[]{att.getMeasurementAreaId(), att.getMeasurementAreaId()};
    }
}
