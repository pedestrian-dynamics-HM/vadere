package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.util.geometry.shapes.VPoint;

public interface IPointDensityAlgorithm {
	String getName();
	double getDensity(final VPoint pos, final SimulationState state);
}
