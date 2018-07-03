package org.vadere.util.geometry;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VShape;

public class GeometryUtils {
	/**
	 * Constant for comparison of double values. Everything below this is
	 * considered equal.
	 */
	public static final double DOUBLE_EPS = 1e-8;

	/**
	 * Interpolates between start and end with the given factor.
	 */
	public static VPoint interpolate(VPoint start, VPoint end, double factor) {
		VPoint result = new VPoint(start.x + factor * (end.x - start.x),
				start.y + factor * (end.y - start.y));
		return result;
	}

	public static boolean collectionContains(
			Collection<? extends VShape> collection, VPoint point) {
		for (VShape shape : collection) {
			if (shape.contains(point)) {
				return true;
			}
		}
		return false;
	}

	public static List<VPoint> getDiscDiscretizationPoints(
			@NotNull final Random random,
			final boolean varyDirection,
			@NotNull final VCircle circle,
			final int numberOfCircles,
			final int numberOfPointsOfLargestCircle,
			final double anchorAngle,
			final double angle) {
		double randOffset = varyDirection ? random.nextDouble() : 0;

		List<VPoint> reachablePositions = new ArrayList<>();

		// iterate through all circles
		for (int j = 1; j <= numberOfCircles; j++) {

			double circleOfGrid = circle.getRadius() * j / numberOfCircles;

			int numberOfGridPoints = (int) Math.ceil(circleOfGrid / circle.getRadius() * numberOfPointsOfLargestCircle);

			// reduce number of grid points proportional to the constraint of direction
			if (angle < 2 * Math.PI) {
				numberOfGridPoints = (int) Math.ceil(numberOfGridPoints * angle / (2 * Math.PI));
			}

			double angleDelta = angle / numberOfGridPoints;

			// iterate through all angles and compute absolute positions of grid points
			for (int i = 0; i < numberOfGridPoints; i++) {

				double x = circleOfGrid * Math.cos(anchorAngle + angleDelta * (randOffset + i)) + circle.getCenter().getX();
				double y = circleOfGrid * Math.sin(anchorAngle + angleDelta * (randOffset + i)) + circle.getCenter().getY();
				VPoint tmpPos = new VPoint(x, y);

				reachablePositions.add(tmpPos);
			}
		}
		return reachablePositions;
	}

	/**
	 * Computes the point on the line segment that is closest to the given point
	 * point. from:
	 * http://stackoverflow.com/questions/3120357/get-closest-point-to-a-line
	 * 
	 * @param point
	 *        the point to which the counterpart should be computed
	 * @param line
	 *        line representing the segment
	 * @return the point on the line that is closest to p
	 */
	public static VPoint closestToSegment(VLine line, VPoint point) {
		if (new VPoint((Point2D.Double) line.getP1()).equals(point)) {
			return new VPoint(line.x1, line.y1);
		}

		VPoint a2p = new VPoint(point.x - line.x1, point.y - line.y1);
		VPoint a2b = new VPoint(line.x2 - line.x1, line.y2 - line.y1);
		double distAB = a2b.x * a2b.x + a2b.y * a2b.y;
		double a2p_dot_a2b = a2p.x * a2b.x + a2p.y * a2b.y;

		// normalize t to [0,1] to stay on the line segment
		double t = Math.min(1, Math.max(0, a2p_dot_a2b / distAB));
		return new VPoint(line.x1 + a2b.x * t, line.y1 + a2b.y * t);
	}

	/**
	 * Orders a given list angular relative to a given point, starting with
	 * angle 0.
	 * 
	 * @param allPoints
	 * @param center
	 * @return an ordered DataPoint list with the angle of the point as data and
	 *         the original index set.
	 */
	public static List<DataPoint> orderByAngle(List<VPoint> allPoints,
			VPoint center) {
		List<DataPoint> orderedList = new ArrayList<DataPoint>();

		for (int i = 0; i < allPoints.size(); i++) {
			Vector2D p = new Vector2D(allPoints.get(i));
			orderedList.add(new DataPoint(p.x, p.y, p.angleTo(center)));
		}
		// sort by angle
		Collections.sort(orderedList, DataPoint.getComparator());

		return orderedList;
	}

	/**
	 * Returns the angle between the x-axis, p1 and p2.
	 * @param p1
	 * @param p2
	 * @return
	 */
	public static double angleTo(VPoint p1, VPoint p2) {
		double atan2 = Math.atan2(p1.y - p2.y, p1.x - p2.x);

		if (atan2 < 0.0) {
			atan2 = Math.PI * 2 + atan2;
		}

		return atan2;
	}

	/**
	 * Calculate the counter clockwise result for the three given points.<br>
	 * ccw(p1,p2,p3) < 0 if p3 is left of Line(p1,p2)<br>
	 * ccw(p1,p2,p3) = 0 if p3 lies on Line(p1,p2)<br>
	 * ccw(p1,p2,p3) > 0 if p3 is right of Line(p1,p2)<br>
	 * 
	 * @param p1
	 *        first point
	 * @param p2
	 *        second point
	 * @param p3
	 *        third point
	 * @return ccw(p1 p2 p3)
	 */
	public static double ccw(VPoint p1, VPoint p2, VPoint p3) {
		return (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x);
	}

	/**
	 * Computes the cross product of two vectors and store it in the cross
	 * vector.
	 * 
	 * @param v1
	 * @param v2
	 * @param cross
	 */
	public static void cross(double[] v1, double[] v2, double[] cross) {
		cross[0] = v1[1] * v2[2] - v1[2] * v2[1];
		cross[1] = v1[2] * v2[0] - v1[0] * v2[2];
		cross[2] = v1[0] * v2[1] - v1[1] * v2[0];
	}

	public static VPolygon polygonFromPoints2D(List<VPoint> vertices) {
		return polygonFromPoints2D(vertices.toArray(new VPoint[0]));
	}

	/**
	 * 
	 * @param vertices
	 * @return
	 */
	public static VPolygon polygonFromPoints2D(VPoint... vertices) {
		Path2D.Double result = new Path2D.Double();
		if (vertices.length == 0)
			return new VPolygon(result);

		VPoint last = vertices[vertices.length - 1];
		result.moveTo(last.getX(), last.getY());

		for (int i = 0; i < vertices.length; i++) {
			result.lineTo(vertices[i].getX(), vertices[i].getY());
		}

		// result.lineTo(first.getX(), first.getY());

		return new VPolygon(result);
	}

	public static VPoint add(VPoint p1, VPoint p2) {
		return new VPoint(p1.x + p2.x, p1.y + p2.y);
	}

	public static VPoint lineIntersectionPoint(final double x1,
	                                           final double y1,
	                                           final double x2,
	                                           final double y2,
	                                           final double x3,
	                                           final double y3,
			                                   final double x4,
			                                   final double y4) {
		assert new VLine(new VPoint(x1,y1), new VPoint(x2, y2)).intersectsLine(x3, y3, x4, y4);
		double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
		assert d != 0;

		double x = ((x1 * y2 - y1 - x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
		double y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (y3 * y4 - y3 * x4)) / d;
		return new VPoint(x, y);
	}

	public static VPoint lineIntersectionPoint(final VPoint p1, final VPoint p2, final VPoint q1, final VPoint q2) {
		return lineIntersectionPoint(p1.x, p1.y, p2.x, p2.y, q1.x, q1.y, q2.x, q2.y);
	}

	public static VPoint lineIntersectionPoint(final VLine line,
	                                           final double x3,
	                                           final double y3,
	                                           final double x4,
	                                           final double y4) {
		return lineIntersectionPoint(line.getX1(), line.getY1(), line.getX2(), line.getY2(), x3, y3, x4, y4);
	}
}
