package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;

/**
 * This data processor returns the number of existing aerosolClouds per timeStep
 */
@DataProcessorClass(label = "AerosolCloudCountingProcessor")
public class AerosolCloudCountingProcessor extends  DataProcessor<TimestepKey, Integer>{
    public AerosolCloudCountingProcessor() {
        super("numberOfAerosolClouds");
    }

    @Override
    public void doUpdate(final SimulationState state) {
        int aerosolCloudCount = state.getTopography().getAerosolClouds().size();
        this.putValue(new TimestepKey(state.getStep()), aerosolCloudCount);
    }
}
