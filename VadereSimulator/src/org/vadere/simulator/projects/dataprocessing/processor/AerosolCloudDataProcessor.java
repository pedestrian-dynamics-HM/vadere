package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepIdDataKey;
import org.vadere.state.scenario.AerosolCloud;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Simon Rahn
 * This processor returns the area, pathogenLoad, and pathogen concentration for each aerosolCloud over time.
 */
@DataProcessorClass()
public class AerosolCloudDataProcessor extends DataProcessor<TimestepIdDataKey, Triple<Double, Double, Double>> {
    public AerosolCloudDataProcessor() {
        super("area", "pathogenLoad", "pathogenConcentration");
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

            double area = cloud.getArea();
            double load = cloud.getCurrentPathogenLoad();
            double concentration = load / (cloud.getHeight() * area);
            putValue(key, Triple.of(area, load, concentration));
        }
    }

    @Override
    public String[] toStrings(@NotNull final  TimestepIdDataKey key) {
        Triple<Double, Double, Double> times = getValue(key);
        return new String[]{Double.toString(times.getLeft()), Double.toString(times.getMiddle()), Double.toString(times.getRight())};
    }
}
