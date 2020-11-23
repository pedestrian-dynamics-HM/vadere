package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;

import java.util.Locale;

/**
 * Log the time when a pedestrian reaches its final target. If a pedestrian does not reach the target
 * before the simulation ends, log Double.POSITIVE_INFINITY (i.e., we enter the post loop and the
 * topography still holds pedestrians).
 *
 * Use a simple map with pedestrian.getId() as key and update the value in each simulation step.
 * This is a naive and inefficient approach, but it works.
 */
@DataProcessorClass()
public class PedestrianTargetReachTimeProcessor extends DataProcessor<PedestrianIdKey, String> {
    public PedestrianTargetReachTimeProcessor() {
        super("targetId reachTime");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        state.getTopography().getElements(Pedestrian.class).stream()
                .forEach(pedestrian -> {
                    PedestrianIdKey pedId = new PedestrianIdKey(pedestrian.getId());
                    int targetId = (pedestrian.hasNextTarget()) ? pedestrian.getNextTargetId() : -1;
                    String targetAndReachTime = String.format(Locale.US,"%s %s", targetId, state.getSimTimeInSec());
                    this.putValue(pedId, targetAndReachTime);
                });
    }

    @Override
    public void postLoop(final SimulationState state) {
        state.getTopography().getElements(Pedestrian.class).stream()
                .forEach(pedestrian -> {
                    PedestrianIdKey pedId = new PedestrianIdKey(pedestrian.getId());
                    int targetId = (pedestrian.hasNextTarget()) ? pedestrian.getNextTargetId() : -1;
                    String targetAndReachTime = String.format(Locale.US,"%s %s", targetId, Double.POSITIVE_INFINITY);
                    this.putValue(pedId, targetAndReachTime);
                });
    }

    @Override
    public void init(final ProcessorManager manager) {
       super.init(manager);
    }

}
