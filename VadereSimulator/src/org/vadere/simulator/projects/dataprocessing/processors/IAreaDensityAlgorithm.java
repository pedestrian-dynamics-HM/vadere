package org.vadere.simulator.projects.dataprocessing.processors;

import org.vadere.simulator.control.SimulationState;

public interface IAreaDensityAlgorithm {
    String getName();
    double getDensity(final SimulationState state);
}
