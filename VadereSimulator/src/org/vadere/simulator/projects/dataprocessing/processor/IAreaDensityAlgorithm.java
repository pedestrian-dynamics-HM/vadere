package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.simulation.SimulationState;

/**
 * @author Mario Teixeira Parente
 *
 */

public interface IAreaDensityAlgorithm {
    String getName();
    double getDensity(final SimulationState state);
}
