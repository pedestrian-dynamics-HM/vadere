package org.vadere.simulator.models.potential.solver.gradients;

import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.MathUtil;

/**
 * Provides gradients for a ring / ellipsoid structure.
 * 
 */
public class FloorGradientProviderRingContinuous implements GradientProvider {

	private static final double CUTOFF = 0.2;
	private static final double OBSTACLE_HEIGHT = 30;
	private double radiusSmall;
	private double radiusBig;
	private double xStretch;
	private boolean directionCW;
	private VPoint center;

	public FloorGradientProviderRingContinuous(VPoint center,
			double radiusSmall, double radiusBig, double xStretch,
			double yStretch) {
		this(center, radiusSmall, radiusBig, xStretch, yStretch, false);
	}

	public FloorGradientProviderRingContinuous(VPoint center,
			double radiusSmall, double radiusBig, double xStretch,
			double yStretch, boolean directionCCW) {
		this.center = center;
		this.radiusSmall = radiusSmall;
		this.radiusBig = radiusBig;
		this.xStretch = xStretch;
		this.directionCW = directionCCW;
	}

	@Override
	public void gradient(double t, int currentTargetId, double[] x, double[] grad) {
		double[] localPos = new double[2];
		double[] localCenter = new double[2];

		// shift center to the right side
		if ((x[0] - this.center.x) > this.xStretch) {
			localCenter[0] = this.center.x + this.xStretch;
			localCenter[1] = this.center.y;
		} else {
			localCenter[0] = this.center.x - this.xStretch;
			localCenter[1] = this.center.y;
		}

		// shift position to local coordinate system around the precomputed
		// center
		localPos[0] = x[0] - localCenter[0];
		localPos[1] = x[1] - localCenter[1];

		// check whether the given position x is close to the straight parts.
		// if it is, consider a rectangle for gradient generation; otherwise,
		// consider a circle.
		if (Math.abs(x[0] - this.center.x) <= this.xStretch) {
			// compute default gradient
			grad[0] = Math.signum(localPos[1]) * (this.directionCW ? -1 : 1);
			grad[1] = 0;
			// compute and add border gradient
			double distance = Math.abs(localPos[1]);
			double distToRadiusBig = Math.abs(distance - radiusBig);
			grad[1] += Math.signum(localPos[1])
					* OBSTACLE_HEIGHT
					* MathUtil.cutExp(distToRadiusBig,
							FloorGradientProviderRingContinuous.CUTOFF);
			double distToRadiusSmall = Math.abs(distance - radiusSmall);
			grad[1] += -Math.signum(localPos[1])
					* OBSTACLE_HEIGHT
					* MathUtil.cutExp(distToRadiusSmall,
							FloorGradientProviderRingContinuous.CUTOFF);

			if (Math.abs(localPos[1]) > radiusBig) {
				grad[0] = 0;
				grad[1] = OBSTACLE_HEIGHT * Math.signum(localPos[1]);
			}
			if (Math.abs(localPos[1]) < radiusSmall) {
				grad[0] = 0;
				grad[1] = -OBSTACLE_HEIGHT * Math.signum(localPos[1]);
			}
			return;
		}

		// find angle3D
		double phi = Math.atan2(localPos[1], localPos[0]) + Math.PI;

		// if the direction should be clockwise, invert phi
		if (this.directionCW) {
			phi = -phi;
		}

		// compute gradient based on angle3D
		grad[0] = -Math.sin(phi);
		grad[1] = Math.cos(phi);

		// norm to one
		double gradNorm = MathUtil.norm2(grad);
		grad[0] /= gradNorm;
		grad[1] /= gradNorm;

		// find distance to center
		double distance = MathUtil.norm2(localPos);

		// add ring properties as "obstacles" at the borders
		if (distance > this.radiusBig
				- FloorGradientProviderRingContinuous.CUTOFF) {
			double distToRadius = Math.abs(distance - radiusBig);
			grad[0] += (localPos[0])
					* OBSTACLE_HEIGHT
					* MathUtil.cutExp(distToRadius,
							FloorGradientProviderRingContinuous.CUTOFF);
			grad[1] += (localPos[1])
					* OBSTACLE_HEIGHT
					* MathUtil.cutExp(distToRadius,
							FloorGradientProviderRingContinuous.CUTOFF);
		}
		if (distance > this.radiusBig) {
			grad[0] += (localPos[0]) * OBSTACLE_HEIGHT;
			grad[1] += (localPos[1]) * OBSTACLE_HEIGHT;
		}
		if (distance < this.radiusSmall
				+ FloorGradientProviderRingContinuous.CUTOFF) {
			double distToRadius = Math.abs(distance - radiusSmall);
			grad[0] += (-localPos[0])
					* OBSTACLE_HEIGHT
					* MathUtil.cutExp(distToRadius,
							FloorGradientProviderRingContinuous.CUTOFF);
			grad[1] += (-localPos[1])
					* OBSTACLE_HEIGHT
					* MathUtil.cutExp(distToRadius,
							FloorGradientProviderRingContinuous.CUTOFF);
		}
		if (distance < this.radiusSmall) {
			grad[0] += (-localPos[0]) * OBSTACLE_HEIGHT;
			grad[1] += (-localPos[1]) * OBSTACLE_HEIGHT;
		}
	}

}
