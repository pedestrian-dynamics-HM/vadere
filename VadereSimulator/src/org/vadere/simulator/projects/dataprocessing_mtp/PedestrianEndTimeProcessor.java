package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

public class PedestrianEndTimeProcessor extends Processor<PedestrianIdDataKey, Double> {
    public PedestrianEndTimeProcessor() {
        super("tend");
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
    void init(final AttributesProcessor attributes, final ProcessorManager manager) {
        // No initialization needed
    }

    private void setValues(Collection<Pedestrian> peds, double value) {
        peds.stream().map(ped -> new PedestrianIdDataKey(ped.getId()))
                .forEach(key -> this.addValue(key, value));
    }
}
