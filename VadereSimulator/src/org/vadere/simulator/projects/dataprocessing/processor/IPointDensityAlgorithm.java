package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Mario Teixeira Parente
 *
 */

public interface IPointDensityAlgorithm {
	String getName();
	double getDensity(final VPoint pos, final SimulationState state);
}
