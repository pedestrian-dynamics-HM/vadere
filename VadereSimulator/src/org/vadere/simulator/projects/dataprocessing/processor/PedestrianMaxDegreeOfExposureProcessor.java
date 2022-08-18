package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;

import java.util.Locale;

/**
 * Log the degree of exposure when a pedestrian reaches its final target. If a pedestrian does not reach the target
 * before the simulation ends, log current degree of exposure (i.e., we enter the post loop and the topography still
 * holds pedestrians).
 *
 * Use a simple map with pedestrian.getId() as key and update the value in each simulation step.
 * This is a naive and inefficient approach, but it works.
 */
@DataProcessorClass()
public class PedestrianMaxDegreeOfExposureProcessor extends DataProcessor<PedestrianIdKey, String> {
    public PedestrianMaxDegreeOfExposureProcessor() {
        super("degreeOfExposure");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        state.getTopography().getElements(Pedestrian.class).stream()
                .forEach(pedestrian -> {
                    if (!pedestrian.isInfectious()) {
                        PedestrianIdKey pedId = new PedestrianIdKey(pedestrian.getId());
                        this.putValue(pedId, getDegreeOfExposure(pedestrian));
                    }
                });
    }

    @Override
    public void postLoop(final SimulationState state) {
        state.getTopography().getElements(Pedestrian.class).stream()
                .forEach(pedestrian -> {
                    if(!pedestrian.isInfectious()) {
                        PedestrianIdKey pedId = new PedestrianIdKey(pedestrian.getId());
                        this.putValue(pedId, getDegreeOfExposure(pedestrian));
                    }
                });
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
    }

    private String getDegreeOfExposure(Pedestrian pedestrian) {
        double degreeOfExposure = (pedestrian.hasNextTarget()) ? pedestrian.getDegreeOfExposure() : -1;
        return String.format(Locale.US,"%s", degreeOfExposure);
    }
}