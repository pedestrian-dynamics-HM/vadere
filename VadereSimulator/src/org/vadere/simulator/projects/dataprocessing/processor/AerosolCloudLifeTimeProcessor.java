package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.IdDataKey;
import org.vadere.state.scenario.AerosolCloud;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Simon Rahn
 * returns creationTime and deletionTime for each aerosolCloud
 */

@DataProcessorClass()
public class AerosolCloudLifeTimeProcessor extends DataProcessor<IdDataKey, Pair<Double, Double>>{
    double simTimeStepLength;
    double finishTime;

    public AerosolCloudLifeTimeProcessor() {
        super("creationTime", "deletionTime");
    }

    @Override
    public void preLoop(SimulationState state) {
        simTimeStepLength = state.getScenarioStore().getAttributesSimulation().getSimTimeStepLength();
        finishTime = state.getScenarioStore().getAttributesSimulation().getFinishTime();
    }

    @Override
    protected void doUpdate(SimulationState state) {
        Collection<AerosolCloud> clouds = new HashSet<>(state.getTopography().getAerosolClouds());
        for(AerosolCloud cloud : clouds) {
            IdDataKey key = new IdDataKey(cloud.getId());

            setCreationTime(key, cloud.getCreationTime());

            double deletionTime = Math.min(state.getSimTimeInSec() + simTimeStepLength, finishTime);
            setDeletionTime(key, deletionTime);
        }
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
    }

    @Override
    public String[] toStrings(@NotNull final  IdDataKey key) {
        Pair<Double, Double> times = getValue(key);
        return new String[]{Double.toString(times.getLeft()), Double.toString(times.getRight())};
    }

    private void setCreationTime(@NotNull final IdDataKey key, double time) {
        putValue(key, Pair.of(time, Double.POSITIVE_INFINITY));
    }

    private void setDeletionTime(@NotNull final IdDataKey key, double time) {
        putValue(key, Pair.of(getValue(key).getLeft(), time));
    }
}
