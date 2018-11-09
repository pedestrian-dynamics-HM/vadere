package org.vadere.simulator.control.util;

import org.vadere.util.geometry.shapes.VShape;

import java.util.List;

@FunctionalInterface
public interface SpawnOverlapCheck {
	/**
	 * Check if the candidateShape can be placed without overlaping any blockPedestrianShapes. Note:
	 * The implementation can also test other Shapes such as obstacles if this is needed.
	 *
	 * @param candidateShape        VShape to be placed in new source
	 * @param blockPedestrianShapes List of VShapes the candidateShape must not overlap with
	 * @return true if candidateShape does not overlap blockPedestrianShapes false if an
	 * overlap occurs.
	 */
	boolean checkFreeSpace(VShape candidateShape, List<VShape> blockPedestrianShapes);
}
