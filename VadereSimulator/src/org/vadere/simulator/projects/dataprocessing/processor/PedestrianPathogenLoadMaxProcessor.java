package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;

import java.util.Locale;

/**
 * Log the absorbed pathogen load when a pedestrian reaches its final target (i.e. the maximum load). If a pedestrian
 * does not reach the target before the simulation ends, log current pathogen load (i.e., we enter the post loop and the
 * topography still holds pedestrians).
 *
 * Use a simple map with pedestrian.getId() as key and update the value in each simulation step.
 * This is a naive and inefficient approach, but it works.
 */
@DataProcessorClass()
public class PedestrianPathogenLoadMaxProcessor extends DataProcessor<PedestrianIdKey, String> {
    public PedestrianPathogenLoadMaxProcessor() {
        super("pathogenLoad");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        state.getTopography().getElements(Pedestrian.class).stream()
                .forEach(pedestrian -> {
                    PedestrianIdKey pedId = new PedestrianIdKey(pedestrian.getId());
                    this.putValue(pedId, getAbsorbedPathogenLoad(pedestrian));
                });
    }

    @Override
    public void postLoop(final SimulationState state) {
        state.getTopography().getElements(Pedestrian.class).stream()
                .forEach(pedestrian -> {
                    PedestrianIdKey pedId = new PedestrianIdKey(pedestrian.getId());
                    this.putValue(pedId, getAbsorbedPathogenLoad(pedestrian));
                });
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
    }

    private String getAbsorbedPathogenLoad(Pedestrian pedestrian) {
        double pathogenLoad = (pedestrian.hasNextTarget()) ? pedestrian.getPathogenAbsorbedLoad() : -1;
        return String.format(Locale.US,"%s", pathogenLoad);
    }
}