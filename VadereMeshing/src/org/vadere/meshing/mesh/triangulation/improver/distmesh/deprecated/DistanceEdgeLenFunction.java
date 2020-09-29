package org.vadere.meshing.mesh.triangulation.improver.distmesh.deprecated;


import org.vadere.util.math.IDistanceFunction;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Collection;

/**
 * @author Matthias Laubinger
 */
@Deprecated
public class DistanceEdgeLenFunction implements IEdgeLengthFunction {

	private final VRectangle boundingBox;
	private final Collection<? extends VShape> obstacles;
	private final IDistanceFunction distanceFunc;

	public DistanceEdgeLenFunction(final VRectangle boundingBox,
	                               final Collection<? extends VShape> obstacles,
	                               final IDistanceFunction distanceFunc) {
		this.boundingBox = boundingBox;
		this.obstacles = obstacles;
		this.distanceFunc = distanceFunc;
	}

	@Override
	public Double apply(IPoint iPoint) {
		double max = Math.max(boundingBox.getWidth(), boundingBox.getHeight());
		boolean smallScenario = max <= 10;

		double result = 0.06 - 0.2 * boundingBox.distance(iPoint);
		double last = -boundingBox.distance(iPoint);

		for (VShape obstacle : obstacles) {
			if (smallScenario) {
				result = distanceFunc.doDUnion(result, 0.06 + 0.2 * obstacle.distance(iPoint));
				last += obstacle.distance(iPoint);
			} else {
				result = distanceFunc.doDUnion(result, 0.06 + 0.2 * obstacle.distance(iPoint) * 10 / max);
				last += obstacle.distance(iPoint) * 10 / max;
			}
		}
		last /= obstacles.size();
		return distanceFunc.doDUnion(result, last);
	}
}
