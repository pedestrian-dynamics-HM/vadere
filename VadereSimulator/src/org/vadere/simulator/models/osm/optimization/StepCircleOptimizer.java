package org.vadere.simulator.models.osm.optimization;

import java.awt.Shape;

import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * The Interface StepCircleOptimizer.
 * 
 */
public interface StepCircleOptimizer {

	/** Returns the reachable position with the minimal potential. */
	VPoint getNextPosition(PedestrianOSM pedestrian, Shape reachableArea);

	public StepCircleOptimizer clone();
}
