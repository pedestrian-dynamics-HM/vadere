package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.state.attributes.processor.AttributesMaxAreaDensityVoronoiProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.OptionalDouble;

/**
 * Saves the maximum of the Voronoi density over the whole simulation time -> scalar output.
 * Used for UQ analysis.
 */

@DataProcessorClass()
public class MaxAreaDensityVoronoiProcessor extends NoDataKeyProcessor<Double> {
    private AreaDensityVoronoiProcessor areaDensityVoronoiProcessor;

    public MaxAreaDensityVoronoiProcessor() {
        super("max_area_density_voronoi_processor");
        setAttributes(new AttributesMaxAreaDensityVoronoiProcessor());
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        //ensure that all required DataProcessors are updated.
        this.areaDensityVoronoiProcessor.update(state);
    }

    @Override
    public void postLoop(final SimulationState state) {
        this.areaDensityVoronoiProcessor.postLoop(state);

        OptionalDouble maxDensity = this.areaDensityVoronoiProcessor.getData().values().stream().mapToDouble(Double::doubleValue).max();
        if(maxDensity.isPresent()) {
            this.putValue(NoDataKey.key(), maxDensity.getAsDouble());
        }
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesMaxAreaDensityVoronoiProcessor att = (AttributesMaxAreaDensityVoronoiProcessor) this.getAttributes();
        this.areaDensityVoronoiProcessor = (AreaDensityVoronoiProcessor) manager.getProcessor(att.getPedestrianAreaDensityVoronoiProcessorId());
    }


    @Override
    public AttributesProcessor getAttributes() {
        if (super.getAttributes() == null) {
            setAttributes(new AttributesMaxAreaDensityVoronoiProcessor());
        }
        return super.getAttributes();
    }
}
