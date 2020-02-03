package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.state.attributes.processor.AttributesMeanAreaDensityVoronoiProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.OptionalDouble;
import java.util.stream.Stream;

/**
 * Saves the mean of the Voronoi density over the whole simulation time -> scalar output.
 * Used for UQ analysis.
 */

@DataProcessorClass()
public class MeanAreaDensityVoronoiProcessor  extends NoDataKeyProcessor<Double> {
    private AreaDensityVoronoiProcessor areaDensityVoronoiProcessor;

    public MeanAreaDensityVoronoiProcessor() {
        super("mean_area_density_voronoi_processor");
        setAttributes(new AttributesMeanAreaDensityVoronoiProcessor());
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        //ensure that all required DataProcessors are updated.
        this.areaDensityVoronoiProcessor.update(state);
    }

    @Override
    public void postLoop(final SimulationState state) {
        this.areaDensityVoronoiProcessor.postLoop(state);

        OptionalDouble meanDensity = this.areaDensityVoronoiProcessor.getData().values().stream().mapToDouble(Double::doubleValue).average();
        if(meanDensity.isPresent()) {
            this.putValue(NoDataKey.key(), meanDensity.getAsDouble());
        }
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesMeanAreaDensityVoronoiProcessor att = (AttributesMeanAreaDensityVoronoiProcessor) this.getAttributes();
        this.areaDensityVoronoiProcessor = (AreaDensityVoronoiProcessor) manager.getProcessor(att.getPedestrianAreaDensityVoronoiProcessorId());
    }


    @Override
    public AttributesProcessor getAttributes() {
        if (super.getAttributes() == null) {
            setAttributes(new AttributesMeanAreaDensityVoronoiProcessor());
        }
        return super.getAttributes();
    }
}
