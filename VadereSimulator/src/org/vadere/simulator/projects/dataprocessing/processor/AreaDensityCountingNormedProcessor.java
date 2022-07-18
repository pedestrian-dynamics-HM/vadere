package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.attributes.processor.AttributesAreaDensityCountingProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.traci.CompoundObject;
import org.vadere.state.traci.CompoundObjectBuilder;
import org.vadere.state.traci.CompoundObjectProvider;
import org.vadere.state.traci.TraCIDataType;
import java.util.Collection;

@DataProcessorClass(label = "AreaDensityCountingNormedProcessor")
public class AreaDensityCountingNormedProcessor extends AreaDataProcessor<Double> implements CompoundObjectProvider{

        public AreaDensityCountingNormedProcessor() {
            super("areaDensityCountingNormed");
            setAttributes(new AttributesAreaDensityCountingProcessor());
        }

    @Override
    protected void doUpdate(final SimulationState state) {

        // Compute density by counting the pedestrians

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

        // With the area of the shape the density IS normalized to [ped/m^2] "pedCount/area"
        double measurementArea = this.getMeasurementArea().asPolygon().getArea();
        double density = (1.0*pedCount)/measurementArea;

        this.putValue(new TimestepKey(state.getStep()), density);
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
        double lastValue = getValue(getLastKey());
        int measurementAreaId = this.getMeasurementArea().getId();
        return builder.rest()
                .add(TraCIDataType.INTEGER) // measurementAreaId
                .add(TraCIDataType.INTEGER) //  timestep of count
                .add(TraCIDataType.DOUBLE) // countInId
                .build(measurementAreaId,
                        getLastKey().getTimestep(),
                        lastValue);
    }


}
