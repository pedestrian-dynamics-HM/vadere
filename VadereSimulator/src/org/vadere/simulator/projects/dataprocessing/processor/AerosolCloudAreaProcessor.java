package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepIdDataKey;
import org.vadere.state.scenario.AerosolCloud;

import java.util.Collection;
import java.util.HashSet;

/**
 * This processor returns the area of all aerosolClouds over time.
 *
 * @author Simon Rahn
 */

@DataProcessorClass()
public class AerosolCloudAreaProcessor extends DataProcessor<TimestepIdDataKey, Double>{
    public AerosolCloudAreaProcessor() {
        super("aerosolCloudArea");
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
    }

    @Override
    protected void doUpdate(SimulationState state) {
        Collection<AerosolCloud> clouds = new HashSet<>(state.getTopography().getAerosolClouds());
        for(AerosolCloud cloud : clouds) {
            TimestepIdDataKey key = new TimestepIdDataKey(state.getStep(), cloud.getId());

            putValue(key, cloud.getArea());
        }
    }
}
