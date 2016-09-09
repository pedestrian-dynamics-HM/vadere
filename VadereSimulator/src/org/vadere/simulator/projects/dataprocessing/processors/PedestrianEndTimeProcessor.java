package org.vadere.simulator.projects.dataprocessing.processors;

import java.util.Collection;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakeys.PedestrianIdDataKey;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processors.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;

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
    public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
        // No initialization needed
    }

    private void setValues(Collection<Pedestrian> peds, double value) {
        peds.stream().map(ped -> new PedestrianIdDataKey(ped.getId()))
                .forEach(key -> this.addValue(key, value));
    }
}
