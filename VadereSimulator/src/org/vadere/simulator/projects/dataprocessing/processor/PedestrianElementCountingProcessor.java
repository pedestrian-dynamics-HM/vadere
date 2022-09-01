package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.EventTimeKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;

/**
 * This processor stores the number of pedestrians alive in
 * the scene for every timeStep given by the real simulation time.
 * @author Ludwig Jaeck
 */
public class PedestrianElementCountingProcessor extends DataProcessor<EventTimeKey,Integer> {
    @Override
    protected void doUpdate(SimulationState state) {
        var numberOfPeds = state.getTopography()
                .getPedestrianDynamicElements()
                .getElements()
                .size();
        this.putValue(new EventTimeKey(state.getSimTimeInSec()),numberOfPeds);
    }
}
