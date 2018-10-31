package org.vadere.simulator.models.potential.solver.gradients;

import java.util.Map;

import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

/**
 * Provides a floor gradient. It is computed by the directions to the targets of
 * the respective pedestrian, regardless of obstacles (i.e. the direct way). The
 * point x with d(x,target)=0 would give problems when calculating the
 * direction: x/norm(x-target). The mollified version (Koester et al. 2012) is
 * this: x/sqrt((x-target).^2+epsilon^2) with a small epsilon.
 * 
 */
public class FloorGradientProviderEuclideanMollified implements
		GradientProvider {

	private Map<Integer, VShape> targetShapes;
	private double epsilonSquared = 1e-2;

	public FloorGradientProviderEuclideanMollified(Map<Integer, VShape> targets) {
		this.targetShapes = targets;
	}

	@Override
	public void gradient(double t, int currentTargetId, double[] x, double[] grad) {
		VPoint posTarget = VPoint.ZERO;

		// get the target position as closest point of the target region to the
		// ped
		// ScenarioElement targetBody = ((ScenarioElement)
		// (targets.get(targetID).clone()));
		// ((Shape2D)(targetBody.getShape())).grow(-0.2);

		posTarget = targetShapes.get(currentTargetId).closestPoint(
				new VPoint(x[0], x[1]));

		// targets.get(targetID).getBody().distance(new VPoint(x[0],
		// x[1]), posTarget);

		// compute the direction to the closest point of the target region
		// (inverse direction to make it an actual gradient)
		grad[0] = x[0] - posTarget.getX();
		grad[1] = x[1] - posTarget.getY();

		// normalize to one, mollified version
		double norm = Math.sqrt(grad[0] * grad[0] + grad[1] * grad[1]
				+ epsilonSquared);
		if (norm > 0) {
			grad[0] /= norm;
			grad[1] /= norm;
		}
	}

}
