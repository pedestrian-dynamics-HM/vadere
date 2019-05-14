package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.attributes.processor.AttributesAreaDensityCountingProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;

/**
 * @author Daniel Lehmberg
 * Processor counts number of pedestrians in a measurement area of any shape.
 */
@DataProcessorClass(label = "AreaDensityCountingProcessor")
public class AreaDensityCountingProcessor extends AreaDataProcessor<Integer> {

    public AreaDensityCountingProcessor() {
        super("areaDensityCounting");
        setAttributes(new AttributesAreaDensityCountingProcessor());
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        int step = state.getStep();

        int pedCount = 0;
        Collection<Pedestrian> pedestrians = state.getTopography().getPedestrianDynamicElements().getElements();

        for (Pedestrian p : pedestrians) {
            if(this.getMeasurementArea().getShape().contains(p.getPosition())){
                pedCount++;
            }
        }

        this.putValue(new TimestepKey(step), pedCount);
    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesAreaDensityCountingProcessor());
        }
        return super.getAttributes();
    }
}
