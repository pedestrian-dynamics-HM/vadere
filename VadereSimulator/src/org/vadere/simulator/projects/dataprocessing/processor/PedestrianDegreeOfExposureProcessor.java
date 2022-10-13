package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

/**
 * This processor returns the degree of exposure per pedestrian id at each simulation step
 */
@DataProcessorClass()
public class PedestrianDegreeOfExposureProcessor extends DataProcessor<TimestepPedestrianIdKey, Double> {
    public PedestrianDegreeOfExposureProcessor() {
        super("absorbedPathogenLoad");
    }

    @Override
    public void doUpdate(final SimulationState state) {
        Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);
        peds.forEach(p -> {
            if(!p.isInfectious()) {
                this.putValue(new TimestepPedestrianIdKey(state.getStep(), p.getId()), p.getDegreeOfExposure());
            }
        });
    }
}
