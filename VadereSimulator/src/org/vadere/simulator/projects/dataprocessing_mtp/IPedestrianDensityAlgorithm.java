package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.simulator.control.SimulationState;
import org.vadere.util.geometry.shapes.VPoint;

public interface IPedestrianDensityAlgorithm {
	String getName();
	double getDensity(final VPoint pos, final SimulationState state);
}
