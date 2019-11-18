package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.simulation.SimulationState;

/**
 * @author Mario Teixeira Parente
 *
 */

public interface IAreaVelocityAlgorithm {
    String getName();
    double getVelocity(final SimulationState state);
}
