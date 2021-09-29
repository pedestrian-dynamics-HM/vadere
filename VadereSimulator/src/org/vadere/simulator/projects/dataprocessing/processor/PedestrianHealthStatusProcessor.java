package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.health.InfectionStatus;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

/**
 * This processor returns pedestrians' absorbedPathogenLoad and InfectionStatus for each timeStep and pedestrianId
 */
@DataProcessorClass()
public class PedestrianHealthStatusProcessor extends DataProcessor<TimestepPedestrianIdKey, Pair<Double, InfectionStatus>> {
    PedestrianHealthStatusProcessor() {
        super("absorbedPathogenLoad", "InfectionStatus");
    }

    @Override
    public void doUpdate(final SimulationState state) {
        Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);
        for(Pedestrian ped : peds) {
            TimestepPedestrianIdKey key = new TimestepPedestrianIdKey(state.getStep(), ped.getId());

            setAbsorbedLoad(key, ped.getPathogenAbsorbedLoad());
            setInfectionStatus(key, ped.getInfectionStatus());
        }
    }

    @Override
    public String[] toStrings(@NotNull final  TimestepPedestrianIdKey key) {
        Pair<Double, InfectionStatus> times = getValue(key);
        return new String[]{Double.toString(times.getLeft()), times.getRight().toString()};
    }

    private void setAbsorbedLoad(@NotNull final TimestepPedestrianIdKey key, double time) {
        putValue(key, Pair.of(time, InfectionStatus.RECOVERED)); // InfectionStatus.RECOVERED is just a place holder
    }

    private void setInfectionStatus(@NotNull final TimestepPedestrianIdKey key, InfectionStatus infectionStatus) {
        putValue(key, Pair.of(getValue(key).getLeft(), infectionStatus));
    }
}
