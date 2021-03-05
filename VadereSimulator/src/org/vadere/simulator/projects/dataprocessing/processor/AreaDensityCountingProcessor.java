package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.attributes.processor.AttributesAreaDensityCountingProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;
import java.util.Collection;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.state.traci.CompoundObject;
import org.vadere.state.traci.CompoundObjectBuilder;
import org.vadere.state.traci.CompoundObjectProvider;
import org.vadere.state.traci.TraCIDataType;

/**
 * @author Daniel Lehmberg
 * Processor counts number of pedestrians in a measurement area of any shape.
 *
 * To avoid unneccessary complicated structures this class does *not* extend from @AreaDensityProcessor (which requires
 * an intern class or a separate class implementing the density algorithm). However, AreaDensityProcessor is a better
 * description of what the processor does.
 * See e.g. AreaDensityVoronoiProcessor / AreaDensityVoronoiAlgorithm
 */

@DataProcessorClass(label = "AreaDensityCountingProcessor")
public class AreaDensityCountingProcessor extends AreaDataProcessor<Integer> implements CompoundObjectProvider {

    public AreaDensityCountingProcessor() {
        super("areaDensityCounting");
        setAttributes(new AttributesAreaDensityCountingProcessor());
    }

    @Override
    protected void doUpdate(final SimulationState state) {

        // Compute density by counting the pedestrians
        // With the area of the shape the density can be normalized to [ped/m^2] "pedCount/area"
        int pedCount = 0;

        // Here could also be another processor. However, because a processor uses more memory, all pedestrians
        // are collected from the state directly.
        Collection<Pedestrian> pedestrians = state.getTopography().getPedestrianDynamicElements().getElements();

        // Alternatively, this could be implemented with "Streams"
        for (Pedestrian p : pedestrians) {
            if(this.getMeasurementArea().getShape().contains(p.getPosition())){
                pedCount++;
            }
        }

        this.putValue(new TimestepKey(state.getStep()), pedCount);
    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesAreaDensityCountingProcessor());
        }
        return super.getAttributes();
    }

    @Override
    public CompoundObject provide(CompoundObjectBuilder builder) {
        int lastValue = getValue(getLastKey());
        int measurementAreaId = this.getMeasurementArea().getId();
        return builder.rest()
                .add(TraCIDataType.INTEGER) // measurementAreaId
                .add(TraCIDataType.INTEGER) //  timestep of count
                .add(TraCIDataType.INTEGER) // countInId
                .build(measurementAreaId,
                        getLastKey().getTimestep(),
                        lastValue);
    }
}
