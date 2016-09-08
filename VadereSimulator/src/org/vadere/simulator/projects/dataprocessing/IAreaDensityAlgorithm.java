package org.vadere.simulator.projects.dataprocessing;

import org.vadere.simulator.control.SimulationState;

public interface IAreaDensityAlgorithm {
    String getName();
    double getDensity(final SimulationState state);
}
