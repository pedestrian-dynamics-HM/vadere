package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.health.InfectionStatus;
import org.vadere.state.scenario.Pedestrian;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@DataProcessorClass()
public class NumberOfPedPerInfectionStatusProcessor extends DataProcessor<TimestepKey, List<Long>> {

    public NumberOfPedPerInfectionStatusProcessor(){
        super("infection statuses placeholder");
        // super("S", "E", "I", "R"); // ToDo: iterate through enum InfectionStatus
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
    }

    @Override
    protected void doUpdate(SimulationState state) {
        Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);
        List<Long> numberOfPed = new ArrayList<>();
        for (InfectionStatus infectionStatus : InfectionStatus.values()) {
            numberOfPed.add(peds.stream().filter(p -> p.getInfectionStatus() == infectionStatus).count());

        }
        putValue(new TimestepKey(state.getStep()), numberOfPed);
    }

}

