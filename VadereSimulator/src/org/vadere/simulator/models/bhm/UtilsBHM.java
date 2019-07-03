package org.vadere.simulator.models.bhm;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.GrahamScan;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class UtilsBHM {

	private static Logger logger = Logger.getLogger(UtilsBHM.class);

	public final static double DOUBLE_EPSILON = 0.00001;

	// static helpers...

	public static VPoint getTargetStep(PedestrianBHM me, VPoint position, VPoint targetDirection) {
		return position.add(targetDirection.scalarMultiply(me.getStepLength()));
	}

	public static List<VPoint> getTangentialPoints(final VPoint pointOutside, final double radius) {

		List<VPoint> result = new ArrayList<VPoint>(2);

		// point that the tangential line must lead through
		double x0 = pointOutside.x;
		double y0 = pointOutside.y;

		double radiusSqr = radius * radius;

		// solutions
		double x1, y1, x2, y2;

		// cases for numerical stability
		if (y0 < DOUBLE_EPSILON && y0 > -DOUBLE_EPSILON) {

			x1 = radiusSqr / x0;
			x2 = x1;
			y1 = Math.sqrt(radiusSqr - x1 * x1);
			y2 = -y1;

		} else if (x0 < DOUBLE_EPSILON && x0 > -DOUBLE_EPSILON) {

			y1 = radiusSqr / y0;
			y2 = y1;
			x1 = Math.sqrt(radiusSqr - y1 * y1);
			x2 = -x1;

		} else if (x0 < y0) {
			double y0Sqr = y0 * y0;

			// coefficients of the quadratic equation
			double a = (1 + x0 * x0 / y0Sqr);
			double b = -(2 * radiusSqr * x0 / y0Sqr);
			double c = radiusSqr * radiusSqr / y0Sqr - radiusSqr;

			double discriminant = Math.sqrt(b * b - 4 * a * c);

			x1 = (-b + discriminant) / (2 * a);
			x2 = (-b - discriminant) / (2 * a);
			y1 = (radiusSqr - x1 * x0) / y0;
			y2 = (radiusSqr - x2 * x0) / y0;

		} else {
			double x0Sqr = x0 * x0;

			// coefficients of the quadratic equation
			double a = (1 + y0 * y0 / x0Sqr);
			double b = -(2 * radiusSqr * y0 / x0Sqr);
			double c = radiusSqr * radiusSqr / x0Sqr - radiusSqr;

			double discriminant = Math.sqrt(b * b - 4 * a * c);

			y1 = (-b + discriminant) / (2 * a);
			y2 = (-b - discriminant) / (2 * a);
			x1 = (radiusSqr - y1 * y0) / x0;
			x2 = (radiusSqr - y2 * y0) / x0;
		}

		if (Double.isNaN(x1) || Double.isNaN(y1) || Double.isNaN(x2) || Double.isNaN(y2)) {
			logger.error("Could not generate tangential point at (" +
					pointOutside + ") with radius=" + radius + ".");
		}

		result.add(new VPoint(x1, y1));
		result.add(new VPoint(x2, y2));

		return result;
	}

	public static List<VPoint> getRelativeEvasionPointFromTangential(
			VPoint relativePosition, List<VPoint> tangentialPoints) {

		List<VPoint> result = new ArrayList<VPoint>(2);

		// generate new position on tangent line
		result.add(tangentialPoints.get(0).subtract(relativePosition));
		result.add(tangentialPoints.get(1).subtract(relativePosition));

		return result;
	}

	public static double angle(VPoint a, VPoint b) {
		return Math.acos(a.norm().scalarProduct(b.norm()));
	}

	public static double angleTo(VPoint vector1, VPoint vector2) {

		double angle = Math.acos(vector1.x * vector2.x + vector1.y * vector2.y);
		double ccw = vector1.x * vector2.y - vector1.y * vector2.x;

		if (ccw < 0) {
			angle = -angle;
		}

		return angle;
	}

	public static double angleBetweenTargetDirection(PedestrianBHM me, Pedestrian other) {

		if (other.getVelocity().getLength() != 0) {
			return angle(me.getTargetDirection(), other.getVelocity());
		} else {
			return Math.PI;
		}
	}

	public static double angleBetweenLastDirection(PedestrianBHM me, VPoint stepDirection) {

		if (me.getVelocity().getLength() != 0) {
			return UtilsBHM.angle(me.getVelocity(), stepDirection);
		} else {
			return 0;
		}
	}

	public static double angleBetweenMovingDirection(PedestrianBHM me, Pedestrian other) {

		if (me.getVelocity().getLength() != 0 && other.getVelocity().getLength() != 0) {
			return UtilsBHM.angle(me.getVelocity(), other.getVelocity());
		} else {
			return Math.PI;
		}
	}

	public static double angleBetweenTarget(PedestrianBHM me, Pedestrian other) {
		return UtilsBHM.angle(me.getTargetDirection(),
				other.getPosition().subtract(me.getPosition()));
	}

	/**
	 * Filters the list of pedestrians such that only pedestrians will remain which form
	 * the convex hull of all the pedestrians in the list (with repect to their position).
	 *
	 * @param cluster list of pedestrians
	 *
	 * @return pedestrians forming the convex hull
	 */
	private static List<Pedestrian> convexHull(@NotNull final List<Pedestrian> cluster) {
		if(cluster.size() > 2) {
			return cluster;
		}

		GrahamScan grahamScan = new GrahamScan(cluster.stream().map(ped -> ped.getPosition()).collect(Collectors.toList()));
		Set<VPoint> convexHull = new HashSet<>(grahamScan.getConvexHull());
		return cluster.stream().filter(ped -> convexHull.contains(ped.getPosition())).collect(Collectors.toList());
	}

	/**
	 * Given the fractions f1, f2, ..., fn this method returns 1 or 2 ... or n with probability
	 * f1 / sum, f2 / sum, ..., fn / sum for sum = f1 + f2 + ... + fn.
	 *
	 * @param fractions
	 * @param random
	 * @return a randomly chosen index in [0, fraction.size()) with probabilities depending on the fractions
	 */
	public static int randomChoice(@NotNull final List<Double> fractions, @NotNull final Random random) {

		double[] probabilities = new double[fractions.size()];
		double sum = 0;

		for (double d : fractions) {
			sum += d;
		}

		if (sum == 0) {
			throw new IllegalArgumentException("Sum of fractions must not be 0.");
		}

		for (int i = 0; i < fractions.size(); i++) {
			probabilities[i] = fractions.get(i) / sum;
		}

		int result = 1;

		double rand = random.nextDouble();
		sum = 0;

		for (double prob : probabilities) {
			sum += prob;
			if (rand <= sum) {
				break;
			} else {
				result++;
			}
		}
		return result;
	}
}
