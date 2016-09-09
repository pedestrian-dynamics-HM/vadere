package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;

public interface IAreaDensityAlgorithm {
    String getName();
    double getDensity(final SimulationState state);
}
