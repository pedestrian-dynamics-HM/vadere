package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdDataKey;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

public class PedestrianEndTimeProcessor extends DataProcessor<PedestrianIdDataKey, Double> {
    public PedestrianEndTimeProcessor() {
        super("endTime");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        this.setValues(state.getTopography().getElements(Pedestrian.class), state.getSimTimeInSec());
    }

    @Override
    public void postLoop(final SimulationState state) {
        this.setValues(state.getTopography().getElements(Pedestrian.class), Double.NaN);
    }

    @Override
    public void init(final ProcessorManager manager) {
        // No initialization needed
    }

    private void setValues(Collection<Pedestrian> peds, double value) {
        peds.stream().map(ped -> new PedestrianIdDataKey(ped.getId()))
                .forEach(key -> this.addValue(key, value));
    }
}
