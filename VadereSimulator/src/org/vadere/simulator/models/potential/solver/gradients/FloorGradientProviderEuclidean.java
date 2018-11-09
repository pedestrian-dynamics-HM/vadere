package org.vadere.simulator.models.potential.solver.gradients;

import java.util.Map;

import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.MathUtil;

/**
 * Provides a floor gradient. It is computed by the directions to the targets of
 * the respective pedestrian, regardless of obstacles (i.e. the direct way).
 * 
 */
public class FloorGradientProviderEuclidean implements GradientProvider {

	private Map<Integer, VShape> targetShapes;

	public FloorGradientProviderEuclidean(Map<Integer, VShape> targetShapes) {
		this.targetShapes = targetShapes;
	}

	@Override
	public void gradient(double t, int currentTargetId, double[] x,
			double[] grad) {
		if (targetShapes.isEmpty() || targetShapes.get(currentTargetId) == null) {
			grad[0] = 0;
			grad[1] = 0;
			return;
		}

		VPoint posTarget = VPoint.ZERO;
		// get the target position as closest point of the target region to the
		// ped
		posTarget = targetShapes.get(currentTargetId).closestPoint(
				new VPoint(x[0], x[1]));

		double tx = posTarget.x;
		double ty = posTarget.y;

		// compute the direction to the closest point of the target region
		// (inverse direction to make it an actual gradient)
		grad[0] = x[0] - tx;
		grad[1] = x[1] - ty;

		// normalize to one
		double norm = MathUtil.norm2(grad);
		if (norm > 0) {
			grad[0] /= norm;
			grad[1] /= norm;
		}
	}

}
