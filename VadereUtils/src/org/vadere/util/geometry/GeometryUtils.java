package org.vadere.util.geometry;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Random;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;
import java.awt.geom.Area;

/**
 * This utility class contains only static methods which are used to solve
 * general geometric problems like line or shape intersections.
 *
 * @author Benedikt Zoennchen
 */
public class GeometryUtils {

	/**
	 * Constant for comparison of double values. Everything below this is
	 * considered equal.
	 */
	public static final double DOUBLE_EPS = 1e-11;

	public static final Logger log = Logger.getLogger(GeometryUtils.class);

	public static double lengthSq(double x, double y) {
		return x*x + y*y;
	}

	public static double length(double x, double y) {
		return Math.sqrt(lengthSq(x, y));
	}

	/**
	 * Interpolates between start and end with the given factor i.e. two values at once.
	 *
	 * @param start     the start / min values
	 * @param end       the end / max values times factor
	 * @param factor    the scale of the max value
	 *
	 * @return the two interpolated values (x,y)
	 */
	public static VPoint interpolate(@NotNull final VPoint start, @NotNull final VPoint end, double factor) {
		VPoint result = new VPoint(start.x + factor * (end.x - start.x),
				start.y + factor * (end.y - start.y));
		return result;
	}

	/**
	 * Computes the determinant of a 2x2-matrix.
	 * @param x1    x11 of the matrix
	 * @param y1    x21 of the matrix
	 * @param x2    x21 of the matrix
	 * @param y2    x22 of the matrix
	 * @return
	 */
	public static double det2D(double x1, double y1, double x2, double y2) {
		return x1 * y2 - y1 * x2;
	}

	/**
	 * Determinant of a 3x3-matrix
	 *
	 * @param a
	 * @return
	 */
	public static double det3D(final double[][] a) {
		return a[0][0]*a[1][1]*a[2][2] + a[0][1]*a[1][2]*a[2][0] + a[0][2]*a[1][0]*a[2][1] - a[0][2]*a[1][1]*a[2][0] - a[0][1]*a[1][0]*a[2][2] - a[0][0]*a[1][2]*a[2][1];
	}

	/**
	 * Computes the (optional) intersection point of a {@link VRectangle} boundary and the line-segment defined by ((x1, y1), (x2, y2)).
	 *
	 * @param rectangle the rectangle
	 * @param x1        the x-coordinate of the first point of the line-segment
	 * @param y1        the y-coordinate of the first point of the line-segment
	 * @param x2        the x-coordinate of the second point of the line-segment
	 * @param y2        the y-coordinate of the second point of the line-segment
	 *
	 * @return the intersection point or {@link Optional#empty()}
	 */
	public static Optional<VPoint> intersectionPoint(
			@NotNull final VRectangle rectangle,
			final double x1,
			final double y1,
			final double x2,
			final double y2) {
		if(intersectLineSegment(rectangle.x, rectangle.y, rectangle.x + rectangle.width, rectangle.y, x1, y1, x2, y2)) {
			return Optional.of(intersectionPoint(rectangle.x, rectangle.y, rectangle.x + rectangle.width, rectangle.y, x1, y1, x2, y2));
		}
		else if(intersectLineSegment(rectangle.x, rectangle.y, rectangle.x, rectangle.y + rectangle.height, x1, y1, x2, y2)) {
			return Optional.of(intersectionPoint(rectangle.x, rectangle.y, rectangle.x, rectangle.y + rectangle.height, x1, y1, x2, y2));
		}
		else if(intersectLineSegment(rectangle.x + rectangle.width, rectangle.y, rectangle.x + rectangle.width, rectangle.y + rectangle.height, x1, y1, x2, y2)) {
			return Optional.of(intersectionPoint(rectangle.x + rectangle.width, rectangle.y, rectangle.x + rectangle.width, rectangle.y + rectangle.height, x1, y1, x2, y2));
		}
		if(intersectLineSegment(rectangle.x, rectangle.y + rectangle.height, rectangle.x + rectangle.width, rectangle.y + rectangle.height, x1, y1, x2, y2)) {
			return Optional.of(intersectionPoint(rectangle.x, rectangle.y + rectangle.height, rectangle.x + rectangle.width, rectangle.y + rectangle.height, x1, y1, x2, y2));
		}

		return Optional.empty();
	}

	public static VPoint intersectionPoint(@NotNull final VLine line1, @NotNull final VLine line2) {
		return intersectionPoint(line1.x1, line1.y1, line1.x2, line1.y2, line2.x1, line2.y1, line2.x2, line2.y2);
	}

	/**
	 * Tests if there is a intersection between the {@link VRectangle} boundary and the line-segment defined by ((x1, y1), (x2, y2)).
	 *
	 * @param rectangle the rectangle
	 * @param x1        the x-coordinate of the first point of the line-segment
	 * @param y1        the y-coordinate of the first point of the line-segment
	 * @param x2        the x-coordinate of the second point of the line-segment
	 * @param y2        the y-coordinate of the second point of the line-segment
	 *
	 * @return true if there is an intersection, false otherwise.
	 */
	public static boolean intersectsRectangleBoundary(@NotNull final VRectangle rectangle, double x1, double y1, double x2, double y2) {
		return  intersectLineSegment(rectangle.x, rectangle.y, rectangle.x + rectangle.width, rectangle.y, x1, y1, x2, y2) ||
				intersectLineSegment(rectangle.x, rectangle.y, rectangle.x, rectangle.y + rectangle.height, x1, y1, x2, y2) ||
				intersectLineSegment(rectangle.x + rectangle.width, rectangle.y, rectangle.x + rectangle.width, rectangle.y + rectangle.height, x1, y1, x2, y2) ||
				intersectLineSegment(rectangle.x, rectangle.y + rectangle.height, rectangle.x + rectangle.width, rectangle.y + rectangle.height, x1, y1, x2, y2);
	}

	/**
	 * Computes the intersection of two lines (p1,p2) and (q1, q2).
	 * Algorithm from: /http://mathworld.wolfram.com/Line-LineIntersection.html
	 *
	 * Assumption: the lines (p1,p2) and (q1, q2) are not co-linear.
	 *
	 * @param x1 the x-coordinate of p1
	 * @param y1 the y-coordinate of p1
	 * @param x2 the x-coordinate of p2
	 * @param y2 the y-coordinate of p2
	 * @param x3 the x-coordinate of q1
	 * @param y3 the y-coordinate of q1
	 * @param x4 the x-coordinate of q2
	 * @param y4 the y-coordinate of q2
	 *
	 * @return the intersection of two lines (p1,p2) and (q1, q2).
	 */
	public static VPoint intersectionPoint(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
		double a = det2D(x1, x2, y1, y2);
		double b = det2D(x3, x4, y3, y4);
		double c = det2D(x1-x2, x3 - x4, y1 - y2, y3 - y4);

		assert c != 0;

		double x = det2D(a, b, x1 - x2, x3 - x4) / c;
		double y = det2D(a, b, y1 - y2, y3 - y4) / c;

		return new VPoint(x,y);
	}

	/**
	 * Computes the centroid of a poylgon defined by a list of points.
	 *
	 * Assumption: the list [p1, ..., pn] is sorted and for a valid simple polygon and pi != pj
	 * for all points in the list.
	 *
	 * @param polygon a list of points defining the polygon.
	 *
	 * @return the centroid of the polygon
	 */
	public static VPoint getPolygonCentroid(@NotNull final List<? extends IPoint> polygon){
        double area = signedAreaOfPolygon(polygon);
        double xValue = 0;
        double yValue = 0;

        assert polygon.size() > 2;

        int j = 0;
        for (int i = 0; i < polygon.size(); i++) {
        	if(i < polygon.size() - 1) {
        		j = i + 1;
	        }
	        else {
        		j = 0;
	        }

            xValue += (polygon.get(i).getX() + polygon.get(j).getX())
                    * (polygon.get(i).getX() * polygon.get(j).getY()
                    - polygon.get(i).getY() * polygon.get(j).getX());
            yValue += (polygon.get(i).getY() + polygon.get(j).getY())
                    * (polygon.get(i).getX() * polygon.get(j).getY()
                    - polygon.get(i).getY() * polygon.get(j).getX());
        }
        xValue /= (6 * area);
        yValue /= (6 * area);

        if(xValue == Double.NaN || yValue == Double.NaN || area == 0 || area == Double.NaN) {
        	throw new IllegalArgumentException("invalid point list");
        }

        return new VPoint(xValue, yValue);
    }

	/**
	 * Computes the circumcenter of a triangle defined by (p1, p2, p3).
	 *
	 * Assumption: p1 != p2 != p3 != p1.
	 *
	 * @param p1 the first point of the triangle
	 * @param p2 the second point of the triangle
	 * @param p3 the third point of the triangle
	 *
	 * @return the circumcenter of a triangle
	 */
	public static VPoint getCircumcenter(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final IPoint p3) {
		assert !p1.equals(p2) && !p1.equals(p3) && !p2.equals(p3);
		return getCircumcenter(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
	}

	public static VPoint getCircumcenter(final double x1, final double y1, final double x2, final double y2, final double x3, final double y3) {
		double d = 2 * (x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2));
		double x = ((x1 * x1 + y1 * y1) * (y2 - y3)
				+ (x2 * x2 + y2 * y2) * (y3 - y1)
				+ (x3 * x3 + y3 * y3) * (y1 - y2)) / d;
		double y = ((x1 * x1 + y1 * y1) * (x3 - x2)
				+ (x2 * x2 + y2 * y2) * (x1 - x3)
				+ (x3 * x3 + y3 * y3) * (x2 - x1)) / d;

		return new VPoint(x,y);
	}

	/**
	 * Tests if the {@link IPoint} q lies inside the circumcenter of the triangle (p1, p2, p3) by
	 * computing the circumcenter.
	 *
	 * @param p1 the first point of the triangle
	 * @param p2 the second point of the triangle
	 * @param p3 the third point of the triangle
	 * @param q  the point which might lie inside the circumcenter
	 *
	 * @return true if q lies inside the circumcenter, false otherwise
	 */
	public static boolean isInCircumscribedCycle(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final IPoint p3, @NotNull final IPoint q) {
		VPoint circumcenter = getCircumcenter(p1, p2, p3);
		return circumcenter.distance(q) < circumcenter.distance(p1);
	}

	public static VPoint getTriangleMidpoint(final double x1, final double y1, final double x2, final double y2, final double x3, final double y3) {
		return new VPoint((x1 + x2 + x3) / 3.0,
				(y1 + y2 + y3) / 3.0);
	}

	public static List<VPoint> getDiscDiscretizationPoints(
			@NotNull final VCircle circle,
			final int numberOfCircles,
			final int numberOfPointsOfLargestCircle,
			final double anchorAngle,
			final double angle) {
		return getDiscDiscretizationPoints(null, false, circle, numberOfCircles, numberOfPointsOfLargestCircle, anchorAngle, angle);
	}

	public static List<VPoint> getDiscDiscretizationGridPoints(@Nullable final VCircle circle, double edgeLen) {
		int n = (int)(circle.getRadius() * 2 / edgeLen) + 1;
		List<VPoint> points = new ArrayList<>(n * n);
		double x = circle.getCenter().x - circle.getRadius();
		double y = circle.getCenter().y - circle.getRadius();
		for (int i = 0; i < n; i++) {
			for(int j = 0; j < n; j++) {
				VPoint point = new VPoint(x+i * edgeLen, y+j * edgeLen);
				if(circle.contains(point)) {
					points.add(point);
				}
			}
		}
		return points;
	}

	/**
	 * Generates a set of points which are positioned inside a disc segment. The points are placed equidistant on one or multiple circles
	 * with the center at the center of the disc and the radius smaller or equals the radius of the disc.
	 *
	 * @param random                            a random number generator which will only be used if varyDirection is true.
	 * @param varyDirection                     if true the generated points will be rotated by a random offset
	 * @param circle                            the circle defining the disc (containing the points)
	 * @param numberOfCircles                   the number of circles
	 * @param numberOfPointsOfLargestCircle     the number of points of the most outer circle
	 * @param anchorAngle                       start angle3D of the segment
	 * @param angle                             anchorAngle + angle3D = end angle3D of the segment
	 *
	 * @return a set of points which are positioned inside a disc segment
	 */
	public static List<VPoint> getDiscDiscretizationPoints(
			@Nullable final Random random,
			final boolean varyDirection,
			@NotNull final VCircle circle,
			final int numberOfCircles,
			final int numberOfPointsOfLargestCircle,
			final double anchorAngle,
			final double angle) {

		assert random != null || !varyDirection;
		double randOffset = varyDirection ? random.nextDouble() : 0;

		List<VPoint> reachablePositions = new ArrayList<>();

		// iterate through all circles
		for (int j = 1; j <= numberOfCircles; j++) {

			double circleOfGrid = circle.getRadius() * j / numberOfCircles;

			int numberOfGridPoints = (int) Math.ceil(circleOfGrid / circle.getRadius() * numberOfPointsOfLargestCircle);

			// reduce number of grid points proportional to the constraint of direction
			if (angle < 2.0 * Math.PI) {
				numberOfGridPoints = (int) Math.ceil(numberOfGridPoints * angle / (2.0 * Math.PI));
			}

			double angleDelta = angle / numberOfGridPoints;

			// iterate through all angles and compute absolute positions of grid points
			for (int i = 0; i < numberOfGridPoints; i++) {

				double x = circleOfGrid * Math.cos(anchorAngle + angleDelta * (randOffset + i)) + circle.getCenter().getX();
				double y = circleOfGrid * Math.sin(anchorAngle + angleDelta * (randOffset + i)) + circle.getCenter().getY();
				reachablePositions.add(new VPoint(x, y));
			}
		}

		return reachablePositions;
	}


	/**
	 * Computes the point on the line segment that is closest to the given point.
	 * Algorithm from: http://stackoverflow.com/questions/3120357/get-closest-point-to-a-line
	 *
	 * @param point the point to which the counterpart should be computed
	 * @param line line representing the segment
	 *
	 * @return the point on the line that is closest to p
	 */
	public static VPoint closestToSegment(@NotNull final VLine line, @NotNull final IPoint point) {
		VPoint a2p = new VPoint(point.getX() - line.x1, point.getY() - line.y1);
		VPoint a2b = new VPoint(line.x2 - line.x1, line.y2 - line.y1);

		// the line is not a line or a very short line
		if(Math.abs(a2b.x) < GeometryUtils.DOUBLE_EPS && Math.abs(a2b.y) < GeometryUtils.DOUBLE_EPS) {
			return new VPoint(line.x1, line.y1);
		}

		// the point is very close or equal to one of the points of the line
		if(Math.abs(a2p.x) < GeometryUtils.DOUBLE_EPS && Math.abs(a2p.y) < GeometryUtils.DOUBLE_EPS) {
			return new VPoint(point.getX(), point.getY());
		}

		double distAB = a2b.x * a2b.x + a2b.y * a2b.y;
		double a2p_dot_a2b = a2p.x * a2b.x + a2p.y * a2b.y;

		// normalize t to [0,1] to stay on the line segment
		double t = Math.min(1, Math.max(0, a2p_dot_a2b / distAB));
		return new VPoint(line.x1 + a2b.x * t, line.y1 + a2b.y * t);
	}


	/**
	 * Computes the area (it maybe a negative area) of the parallelogram defined by p, q, r.
	 * The area is zero if and only if p, q, r are co-linear.
	 *
	 * @param pX x-coordinate of p
	 * @param pY y-coordinate of p
	 * @param qX x-coordinate of q
	 * @param qY y-coordinate of q
	 * @param rX x-coordinate of r
	 * @param rY y-coordinate of r
	 *
	 * @return area or negative area of the parallelogram defined by p, q, r
	 */
	public static double ccw(final double qX, final double qY, final double pX, final double pY, final double rX, final double rY) {
		return -((qX - pX) * (rY - pY) - (rX - pX) * (qY - pY));
	}

	/**
	 * Computes the area (it maybe a negative area) of the parallelogram defined by p, q, r.
	 * The area is zero if and only if p, q, r are co-linear.
	 *
	 * @param pX x-coordinate of p
	 * @param pY y-coordinate of p
	 * @param qX x-coordinate of q
	 * @param qY y-coordinate of q
	 * @param rX x-coordinate of r
	 * @param rY y-coordinate of r
	 *
	 * @return area or negative area of the parallelogram defined by p, q, r
	 */
	public static double ccwRobust(final double qX, final double qY, final double pX, final double pY, final double rX, final double rY) {
		double result = -((qX - pX) * (rY - pY) - (rX - pX) * (qY - pY));
		if(Math.abs(result) <= DOUBLE_EPS) {
			return 0.0;
		}
		else {
			return result;
		}
	}

	/**
	 * Returns true if q = (xq, yq) is right of the oriented-line defined by (p1 = (x1, y1), p2 = (x2, y2)).
	 *
	 * @param x1 the x-coordinate of p1
	 * @param y1 the y-coordinate of p1
	 * @param x2 the x-coordinate of p2
	 * @param y2 the y-coordinate of p2
	 * @param xq the x-coordinate of q
	 * @param yq the y-coordinate of q
	 *
	 * @return true if q is right of the oriented-line defined by (p1, p2), false otherwise
	 */
	public static boolean isRightOf(final double x1, final double y1, final double x2, final double y2, final double xq, final double yq) {
		return isCW(x1, y1, x2, y2, xq, yq);
	}

	/**
	 * Returns true if q is right of the oriented-line defined by (p1, p2).
	 * @param p1 the start point of the oriented line
	 * @param p2 the end point of the oriented line
	 * @param q  the point which will be tested with respect toe the oriented line
	 *
	 * @return true if q is right of the oriented-line defined by (p1, p2), false otherwise
	 */
	public static boolean isRightOf(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final IPoint q) {
		return isRightOf(p1, p2, q.getX(), q.getY());
	}

	/**
	 * Returns true if q is left of the oriented-line defined by (p1, p2).
	 *
	 * @param p1 the start point of the oriented line
	 * @param p2 the end point of the oriented line
	 * @param q  the point which will be tested with respect toe the oriented line
	 *
	 * @return true if q is left of the oriented-line defined by (p1, p2), false otherwise
	 */
	public static boolean isLeftOf(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final IPoint q) {
		return isLeftOf(p1, p2, q.getX(), q.getY());
	}

	/**
	 * Returns true if q = (x, y) is right of the oriented-line defined by (p1, p2).
	 * @param p1 the start point of the oriented-line
	 * @param p2 the end point of the oriented-line
	 * @param x  x-coordinate of q
	 * @param y  y-coordinate of q
	 *
	 * @return true if q is right of the oriented-line defined by (p1, p2), false otherwise
	 */
	public static boolean isRightOf(@NotNull final IPoint p1, @NotNull final IPoint p2, final double x, final double y) {
		return isCW(p1.getX(), p1.getY(), p2.getX(), p2.getY(), x, y);
	}

	/**
	 * Returns true if q = (xq, yq) is left of the oriented-line defined by (p1 = (x1,y1), p2 = (x2,y2)).
	 * @param x1 x-coordinate of p1
	 * @param y1 y-coordinate of p1
	 * @param x2 x-coordinate of p2
	 * @param y2 y-coordinate of p2
	 * @param xq x-coordinate of q
	 * @param yq y-coordinate of q
	 *
	 * @return true if q is left of the oriented-line defined by (p1, p2), false otherwise
	 */
	public static boolean isLeftOf(final double x1, final double y1, final double x2, final double y2, final double xq, final double yq) {
		return isCCW(x1, y1, x2, y2, xq, yq);
	}

	/**
	 * Returns true if q = (x, y) is left of the oriented-line defined by (p1, p2).
	 *
	 * @param p1 the start point of the oriented line
	 * @param p2 the end point of the oriented line
	 * @param x  x-coordinate of q
	 * @param y  y-coordinate of q
	 *
	 * @return true if q is left of the oriented-line defined by (p1, p2), false otherwise
	 */
	public static boolean isLeftOf(@NotNull final IPoint p1, @NotNull final IPoint p2, final double x, final double y) {
		return isCCW(p1.getX(), p1.getY(), p2.getX(), p2.getY(), x, y);
	}

	/**
	 * Tests if the simple polygon is ccw oriented by testing if 3 points of the polygon are ccw oriented.
	 *
	 * Assumption: the polygon is a valid simple polygon.
	 *
	 * @param polygon a simple polygon
	 *
	 * @return true if the polygon is ccw oriented, false otherwise
	 */
	public static boolean isCCW(final @NotNull VPolygon polygon) {
		List<VPoint> points = polygon.getPath();

		assert points.size() >= 3;

		// find the most-left, most-bottom point of the polygon
		VPoint mostLeft = points.get(0);
		int indexMostLeft = 0;
		for(int i = 1; i < points.size(); i++) {
			if(mostLeft.getX() > points.get(i).getX() ||
					(Double.compare(mostLeft.getX(), points.get(i).getX()) == 0 && mostLeft.getY() > points.get(i).getY())) {
				mostLeft = points.get(i);
				indexMostLeft = i;
			}
		}

		// get the next point and the prev point
		VPoint next = points.get((indexMostLeft+1) % points.size());
		VPoint prev = points.get((indexMostLeft + points.size() - 1) % points.size());

		return isLeftOf(mostLeft, next, prev);

	}

	/**
	 * Returns the angle3D between the x-axis, p1 and p2.
	 *
	 * @param p1 the first point
	 * @param p2 the second point
	 *
	 * @return the angle3D between the x-axis, p1 and p2
	 */
	public static double angleTo(@NotNull final VPoint p1, @NotNull final VPoint p2) {
		double atan2 = Math.atan2(p1.y - p2.y, p1.x - p2.x);

		if (atan2 < 0.0) {
			atan2 = Math.PI * 2 + atan2;
		}

		return atan2;
	}

	public static double angleTo(@NotNull final double p1x,  final double p1y, final double p2x, final double p2y) {
		double atan2 = Math.atan2(p1y - p2y, p1x - p2x);

		if (atan2 < 0.0) {
			atan2 = Math.PI * 2 + atan2;
		}

		return atan2;
	}

	/**
	 * Calculate the counter clockwise result for the three given points.
	 * <ol>
	 *     <li>ccw(p1,p2,p3) smaller than 0 if p3 is left of Line(p1,p2)</li>
	 *     <li>ccw(p1,p2,p3) equals 0 if p3 lies on Line(p1,p2)</li>
	 *     <li>ccw(p1,p2,p3) larger than 0 if p3 is right of Line(p1,p2)</li>
	 * </ol>
	 *
	 * @param p1 first point
	 * @param p2 second point
	 * @param p3 third point
	 *
	 * @return ccw(p1 p2 p3)
	 */
	public static double ccw(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final IPoint p3) {
		return ccwRobust(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
	}

	/**
	 * Tests if q -> p -> r are counter clockwise oriented.
	 *
	 * @param qX x-coordinate of q
	 * @param qY y-coordinate of q
	 * @param pX x-coordinate of p
	 * @param pY y-coordinate of p
	 * @param rX x-coordinate of r
	 * @param rY y-coordinate of r
	 *
	 * @return true if q -> p -> r are counter clockwise oriented, false otherwise
	 */
	public static boolean isCCW(final double qX, final double qY, final double pX, final double pY, final double rX, final double rY) {
		return ccwRobust(qX, qY, pX, pY, rX, rY) > 0;
	}

	/**
	 * Tests if p1 -> p2 -> p3 are counter clockwise oriented.
	 *
	 * @param p1 the first point
	 * @param p2 the second point
	 * @param p3 the third point
	 *
	 * @return true if p1 -> p2 -> p3 are counter clockwise oriented, false otherwise
	 */
	public static boolean isCCW(final IPoint p1, final IPoint p2, final IPoint p3) {
		return isCCW(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
	}

	/**
	 * Tests if q -> p -> r are clockwise oriented.
	 *
	 * @param qX x-coordinate of q
	 * @param qY y-coordinate of q
	 * @param pX x-coordinate of p
	 * @param pY y-coordinate of p
	 * @param rX x-coordinate of r
	 * @param rY y-coordinate of r
	 *
	 * @return true if q -> p -> r are clockwise oriented, false otherwise
	 */
	public static boolean isCW(final double qX, final double qY, final double pX, final double pY, final double rX, final double rY) {
		return ccwRobust(qX, qY, pX, pY, rX, rY) < 0;
	}

	/**
	 * Tests if p1 -> p2 -> p3 are clockwise oriented.
	 *
	 * @param p1 the first point
	 * @param p2 the second point
	 * @param p3 the third point
	 *
	 * @return true if p1 -> p2 -> p3 are clockwise oriented, false otherwise
	 */
	public static boolean isCW(final IPoint p1, final IPoint p2, final IPoint p3) {
		return ccw(p1, p2, p3) < 0;
	}

	/**
	 * Tests if the line-segment (p1, p2) intersects the line (p, q).
	 *
	 * @param p     point defining the line
	 * @param q     point defining the line
	 * @param p1    point defining the line-segment
	 * @param p2    point defining the line-segment
	 *
	 * @return true if the line-segment (p1, p2) intersects the line (p, q), otherwise false.
	 */
	public static boolean intersectLine(@NotNull final IPoint p, @NotNull final IPoint q, @NotNull final IPoint p1, @NotNull final IPoint p2) {
		double ccw1 = ccw(p, q, p1);
		double ccw2 = ccw(p, q, p2);
		return (ccw1 < 0 && ccw2 > 0) || (ccw1 > 0 && ccw2 < 0);
	}

	public static boolean intersectLine(@NotNull final VLine line, @NotNull final IPoint p1, @NotNull final IPoint p2) {
		double ccw1 = ccw(new VPoint(line.getP1()), new VPoint(line.getP2()), p1);
		double ccw2 = ccw(new VPoint(line.getP1()), new VPoint(line.getP2()), p2);
		return (ccw1 < 0 && ccw2 > 0) || (ccw1 > 0 && ccw2 < 0);
	}

	/**
	 * Tests if the line-segment (p1, p2) intersects the line (p, q).
	 *
	 * @param pX    x-coordinate of p
	 * @param pY    y-coordinate of p
	 * @param qX    x-coordinate of q
	 * @param qY    y-coordinate of q
	 * @param p1X   x-coordinate of p1
	 * @param p1Y   y-coordinate of p1
	 * @param p2X   x-coordinate of p2
	 * @param p2Y   y-coordinate of p2
	 *
	 * @return true if the line-segment (p1, p2) intersects the line (p, q), otherwise false.
	 */
	public static boolean intersectLine(final double pX, final double pY, final double qX, final double qY, final double p1X, final double p1Y, final double p2X, final double p2Y) {
		double ccw1 = ccw(pX, pY, qX, qY, p1X, p1Y);
		double ccw2 = ccw(pX, pY, qX, qY, p2X, p2Y);
		return (ccw1 < 0 && ccw2 > 0) || (ccw1 > 0 && ccw2 < 0);
	}

	/**
	 * Tests if the line-segment (p1, p2) intersects the line (p, q).
	 * The <tt>eps</tt> is used to define intersection in case of p1 or p2
	 * being very close to the line to avoid numerical errors.
	 *
	 * @param pX    x-coordinate of p
	 * @param pY    y-coordinate of p
	 * @param qX    x-coordinate of q
	 * @param qY    y-coordinate of q
	 * @param p1X   x-coordinate of p1
	 * @param p1Y   y-coordinate of p1
	 * @param p2X   x-coordinate of p2
	 * @param p2Y   y-coordinate of p2
	 * @param eps   a small distance by which a point close to a line will be regarded as to be on the line
	 *
	 * @return true if the line-segment (p1, p2) intersects the line (p, q), otherwise false.
	 */
	public static boolean intersectLine(final double pX, final double pY, final double qX, final double qY, final double p1X, final double p1Y, final double p2X, final double p2Y, final double eps) {
		double ccw1 = ccw(pX, pY, qX, qY, p1X, p1Y);
		double ccw2 = ccw(pX, pY, qX, qY, p2X, p2Y);
		return (ccw1+eps < 0 && ccw2-eps > 0) || (ccw1-eps > 0 && ccw2+eps < 0);
	}

	/**
	 * Computes the incenter of a triangle (p1, p2, p3).
	 *
	 * Assumption: the three points form a valid triangle.
	 *
	 * @param p1 the first point of the triangle
	 * @param p2 the second point of the triangle
	 * @param p3 the third point of the triangle
	 *
	 * @return the incenter of a triangle (p1, p2, p3)
	 */
	public static VPoint getIncenter(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final IPoint p3) {
		double a = p1.distance(p2);
		double b = p2.distance(p3);
		double c = p3.distance(p1);
		double perimeter = a + b + c;

		VPoint incenter = new VPoint((a * p3.getX() + b * p1.getX() + c * p2.getX()) / perimeter,
				(a * p3.getY() + b * p1.getY() + c * p2.getY()) / perimeter);

		return incenter;
	}

	/**
	 * Computes the incenter circle radius of a triangle (p1, p2, p3).
	 *
	 * Assumption: the three points form a valid triangle.
	 *
	 * @param p1 the first point of the triangle
	 * @param p2 the second point of the triangle
	 * @param p3 the third point of the triangle
	 *
	 * @return the incenter circle radius of a triangle (p1, p2, p3)
	 */
	public static double getIncircleRaduis(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final IPoint p3) {
		double a = p1.distance(p2);
		double b = p2.distance(p3);
		double c = p3.distance(p1);
		double perimeter = a + b + c;
		double s = perimeter / 2.0;

		return Math.sqrt(s * (s-a) * (s-b) * (s-c)) / s;
	}

	public static double qualityInCircleOutCircle(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final IPoint p3) {
		double a = p1.distance(p2);
		double b = p1.distance(p3);
		double c = p3.distance(p2);
		double part;
		if(a != 0.0 && b != 0.0 && c != 0.0) {
			part = ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
		}
		else {
			throw new IllegalArgumentException(new VTriangle(new VPoint(p1), new VPoint(p2), new VPoint(p3)).toString() + " is not a feasible triangle!");
		}
		return part;
	}

	public static double qualityLongestEdgeInCircle(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final IPoint p3) {
		VTriangle tri = new VTriangle(new VPoint(p1), new VPoint(p2), new VPoint(p3));
		double longestEdge = 0.0;
		for(VLine line : tri.getLines()) {
			if(longestEdge < line.length()) {
				longestEdge = line.length();
			}
		}
		assert longestEdge != 0.0;
		return 2 * Math.sqrt(3) * tri.getIncircleRadius() / longestEdge;
	}

	/**
	 * Tests if the half-line-segment starting at p in the direction (q-p) intersects the line-segment (p1,p2).
	 *
	 * @param p     the starting point of the half-line-segment
	 * @param q     the point defining the direction (q-p) of the half-line-segment
	 * @param p1    point defining the line-segment
	 * @param p2    point defining the line-segment
	 *
	 * @return true if the line-segment intersects the  half-line-segment defined, otherwise false.
	 */
	public static boolean intersectHalfLineSegment(@NotNull final IPoint p, @NotNull final IPoint q, @NotNull final IPoint p1, @NotNull final IPoint p2) {
		return intersectHalfLineSegment(p.getX(), p.getY(), q.getX(), q.getY(), p1.getX(), p1.getY(), p2.getX(), p2.getY());
	}

	/*public static boolean intersectHalfLineSegment(final double pX, final double pY, final double qX, final double qY, final double p1X, final double p1Y, final double p2X, final double p2Y) {
		GeometryUtils.distanceToLineSegment()
	}*/

	/**
	 * Tests if the half-line-segment starting at p in the direction (q-p) intersects the line-segment (p1,p2).
	 *
	 * @param pX    x-coordinate of p
	 * @param pY    y-coordinate of p
	 * @param qX    x-coordinate of q
	 * @param qY    y-coordinate of q
	 * @param p1X   x-coordinate of p1
	 * @param p1Y   y-coordinate of p1
	 * @param p2X   x-coordinate of p2
	 * @param p2Y   y-coordinate of p2
	 *
	 * @return true if the line-segment intersects the  half-line-segment defined, otherwise false.
	 */
	public static boolean intersectHalfLineSegment(final double pX, final double pY, final double qX, final double qY, final double p1X, final double p1Y, final double p2X, final double p2Y) {
		double ccw1 = ccw(pX, pY, qX, qY, p1X, p1Y);
		double ccw2 = ccw(pX, pY, qX, qY, p2X, p2Y);

		// p1 and p2 are on different sides of directed line (q,p) if this is not the case there is no intersection
		if((ccw1 < 0 && ccw2 > 0) || (ccw1 > 0 && ccw2 < 0)) {

			double ccwq = ccw(p1X, p1Y, p2X, p2Y, qX, qY);
			double ccwp = ccw(p1X, p1Y, p2X, p2Y, pX, pY);

			// p and q on different sides, therefore the half-segment (q,p) intersects with the line (p1,p2)
			if((ccwq < 0 && ccwp > 0) || (ccwq > 0 && ccwp < 0)) {
				return true;
			} // otherwise p has to be closer to the line-segment p1, p2 than q
			else {
				return GeometryUtils.distanceToLineSegment(p1X, p1Y, p2X, p2Y, qX, qY) < GeometryUtils.distanceToLineSegment(p1X, p1Y, p2X, p2Y, pX, pY);
			}
		}
		else {
			return false;
		}
	}

	/**
	 * Tests if the first line-segment (p,q) intersects the second line-segment (p1,p2).
	 *
	 * @param p     point defining the first line-segment
	 * @param q     point defining the first line-segment
	 * @param p1    point defining the second line-segment
	 * @param p2    point defining the second line-segment
	 *
	 * @return true if the first line-segment intersects the second line-segment, otherwise false.
	 */
	public static boolean intersectLineSegment(@NotNull final IPoint p, @NotNull final IPoint q, @NotNull final IPoint p1, @NotNull final IPoint p2) {
		return intersectLine(p, q, p1, p2) && intersectLine(p1, p2, p, q);
	}

	public static boolean intersectLineSegment(@NotNull VLine line, @NotNull final IPoint p1, @NotNull final IPoint p2) {
		return intersectLine(new VPoint(line.getP1()), new VPoint(line.getP2()), p1, p2) && intersectLine(p1, p2, new VPoint(line.getP1()), new VPoint(line.getP2()));
	}

	/**
	 * Tests if the first line-segment (p,q) intersects the second line-segment (p1,p2).
	 *
	 * @param p     point defining the first line-segment
	 * @param q     point defining the first line-segment
	 * @param p1    point defining the second line-segment
	 * @param p2    point defining the second line-segment
	 *
	 * @return true if the first line-segment intersects the second line-segment, otherwise false.
	 */
	public static boolean intersectLineSegment(@NotNull final Point2D.Double p, @NotNull final Point2D.Double q, @NotNull final Point2D.Double p1, @NotNull final Point2D.Double p2) {
		return intersectLine(p.x, p.y, q.x, q.y, p1.x, p1.y, p2.x, p2.y) && intersectLine(p1.x, p1.y, p2.x, p2.y, p.x, p.y, q.x, q.y);
	}

	/**
	 * Tests if the first line-segment (p = (x1, y1), q = (x2, y2)) intersects the second line-segment (p1 = (x3, y3), p2 = (x4, y4)).
	 *
	 * @param x1 x-coordinate of p
	 * @param y1 y-coordinate of p
	 * @param x2 x-coordinate of q
	 * @param y2 y-coordinate of q
	 * @param x3 x-coordinate of p1
	 * @param y3 y-coordinate of p1
	 * @param x4 x-coordinate of p2
	 * @param y4 y-coordinate of p2
	 *
	 * @return true if the first line-segment intersects the second line-segment, otherwise false.
	 */
	public static boolean intersectLineSegment(final double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
		return intersectLine(x1, y1, x2, y2, x3, y3, x4, y4) && intersectLine(x3, y3, x4, y4, x1, y1, x2, y2);
	}

	/**
	 * Tests if the first line-segment (p = (x1, y1), q = (x2, y2)) intersects the second line-segment (p1 = (x3, y3), p2 = (x4, y4)).
	 *
	 * @param x1 x-coordinate of p
	 * @param y1 y-coordinate of p
	 * @param x2 x-coordinate of q
	 * @param y2 y-coordinate of q
	 * @param x3 x-coordinate of p1
	 * @param y3 y-coordinate of p1
	 * @param x4 x-coordinate of p2
	 * @param y4 y-coordinate of p2
	 *
	 * @return true if the first line-segment intersects the second line-segment, otherwise false.
	 */
	public static boolean isColinear(final double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
		return intersectLine(x1, y1, x2, y2, x3, y3, x4, y4) && intersectLine(x3, y3, x4, y4, x1, y1, x2, y2);
	}

	/**
	 * Tests if the first line-segment (p = (x1, y1), q = (x2, y2)) intersects the second line-segment (p1 = (x3, y3), p2 = (x4, y4)).
	 * The <tt>eps</tt> is used to define intersection in case of p1 or p2
	 * being very close to the line to avoid numerical errors.
	 *
	 * @param x1 x-coordinate of p
	 * @param y1 y-coordinate of p
	 * @param x2 x-coordinate of q
	 * @param y2 y-coordinate of q
	 * @param x3 x-coordinate of p1
	 * @param y3 y-coordinate of p1
	 * @param x4 x-coordinate of p2
	 * @param y4 y-coordinate of p2
	 * @param eps a small distance by which a point close to a line will be regarded as to be on the line
	 *
	 * @return
	 */
	public static boolean intersectLineSegment(final double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, double eps) {
		return intersectLine(x1, y1, x2, y2, x3, y3, x4, y4, eps) && intersectLine(x3, y3, x4, y4, x1, y1, x2, y2, eps);
	}


	/**
	 * Tests if the triangle (p1,p2,p3) contains the point r.
	 *
	 * @param p1    point of the triangle
	 * @param p2    point of the triangle
	 * @param p3    point of the triangle
	 * @param r     point which the triangle might contain.
	 *
	 * @return true if the triangle (p1,p2,p3) contains the point r, otherwise false.
	 */
	public static boolean triangleContains(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final IPoint p3, @NotNull final IPoint r) {
		boolean b1, b2, b3;
		double d1 = GeometryUtils.ccw(r, p1, p2);
		double d2 = GeometryUtils.ccw(r, p2, p3);
		double d3 = GeometryUtils.ccw(r, p3, p1);
		b1 = d1 < 0.0;
		b2 = d2 < 0.0;
		b3 = d3 < 0.0;
		return ((b1 == b2) && (b2 == b3));
	}

	/**
	 * <p>Tests if the circle defined by three non-lin points (p1,p2,p3) contains the point p.
	 * The center of the circle is the circumcenter of the triangle and the radius is equalt to the
	 * distance between the circumcenter and any point of {p1, p2, p3}.</p>
	 *
	 * <p>Assumption: a, b, c are in ccw-order!</p>
	 *
	 * @param a    point of the triangle
	 * @param b    point of the triangle
	 * @param c    point of the triangle
	 * @param p    point which the circle might contain.
	 *
	 * @return true, if the circle defined by three non-lin points (p1,p2,p3) contains the point p, otherwise false
	 */
	public static boolean isInsideCircle(@NotNull final IPoint a, @NotNull final IPoint b, @NotNull final IPoint c, @NotNull final IPoint p) {
		return isInsideCircle(a, b, c, p.getX(), p.getY());
	}

	public static boolean isInsideCircle(@NotNull final IPoint a, @NotNull final IPoint b, @NotNull final IPoint c, @NotNull final IPoint p, double eps) {
		return isInsideCircle(a, b, c, p.getX(), p.getY(), eps);
	}

	/**
	 * Tests whether or not the point (x,y) lies inside the circumcenter of the triangle (a, b, c).
	 * The method is more stable than direct use of the circumcenter since we normalize beforehand.
	 *
	 * Assumption: (a,b,c) form a valid triangle
	 *
	 * @param a the first point of the triangle
	 * @param b the second point of the triangle
	 * @param c the third point of the triangle
	 * @param x x-coordinate of the point of interest
	 * @param y y-coordinate of the point of interest
	 *
	 * @return true if (x,y) lies inside the circumcenter of the triangle (a, b, c), false otherwise
	 */
	public static boolean isInsideCircle(@NotNull final IPoint a, @NotNull final IPoint b, @NotNull final IPoint c, double x , double y, double eps) {
		double adx = a.getX() - x;
		double ady = a.getY() - y;
		double bdx = b.getX() - x;
		double bdy = b.getY() - y;
		double cdx = c.getX() - x;
		double cdy = c.getY() - y;

		double abdet = adx * bdy - bdx * ady;
		double bcdet = bdx * cdy - cdx * bdy;
		double cadet = cdx * ady - adx * cdy;
		double alift = adx * adx + ady * ady;
		double blift = bdx * bdx + bdy * bdy;
		double clift = cdx * cdx + cdy * cdy;

		double disc = alift * bcdet + blift * cadet + clift * abdet;
		return disc > eps;
	}

	public static boolean isInsideCircle(@NotNull final IPoint a, @NotNull final IPoint b, @NotNull final IPoint c, double x , double y) {
		return isInsideCircle(a, b, c, x, y, 0.0);
	}


	/**
	 * Computes the cross product of two vectors and store it in the cross
	 * vector. This is a c-like call.
	 *
	 * @param v1    the first vector
	 * @param v2    the second vector
	 * @param cross
	 */
	public static void cross(@NotNull final double[] v1, @NotNull double[] v2, @NotNull double[] cross) {
		cross[0] = v1[1] * v2[2] - v1[2] * v2[1];
		cross[1] = v1[2] * v2[0] - v1[0] * v2[2];
		cross[2] = v1[0] * v2[1] - v1[1] * v2[0];

	}

	public static double dot(@NotNull final double[] a, @NotNull double[] b) {
		return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
	}

	public static double dot(@NotNull final IPoint a, @NotNull final IPoint b) {
		return a.getX()*b.getY() + a.getY()*b.getY();
	}

	public static double[] norm3D(@NotNull final double[] a, @NotNull double[] b, @NotNull double[] c) {
		double[][] a1 = new double[3][3];
		a1[0][0] = 1; a1[0][1] = a[1]; a1[0][2] = a[2];
		a1[1][0] = 1; a1[1][1] = b[1]; a1[1][2] = b[2];
		a1[2][0] = 1; a1[2][1] = c[1]; a1[2][2] = c[2];

		double[][] a2 = new double[3][3];
		a2[0][0] = a[0]; a2[0][1] = 1; a2[0][2] = a[2];
		a2[1][0] = b[0]; a2[1][1] = 1; a2[1][2] = b[2];
		a2[2][0] = c[0]; a2[2][1] = 1; a2[2][2] = c[2];

		double[][] a3 = new double[3][3];
		a3[0][0] = a[0]; a3[0][1] = a[1]; a3[0][2] = 1;
		a3[1][0] = b[0]; a3[1][1] = b[1]; a3[1][2] = 1;
		a3[2][0] = c[0]; a3[2][1] = c[1]; a3[2][2] = 1;

		double x = det3D(a1);
		double y = det3D(a2);
		double z = det3D(a3);

		return new double[]{x, y, z};
	}

	public static double[] unitNorm(@NotNull final double[] a, @NotNull double[] b, @NotNull double[] c) {
		double[] norm = norm3D(a, b, c);
		double len = len(norm[0], norm[1], norm[2]);
		return new double[]{norm[0] / len, norm[1] / len, norm[2] / len};
	}



	public static void norm3D(@NotNull final double[] v) {
		double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
		assert len > 0;
		if(len > 0) {
			v[0] = v[0] / len;
			v[1] = v[1] / len;
			v[2] = v[2] / len;
		}
	}

	/**
	 * Transforms a list forming a valid simple polygon into a {@link VPolygon} object.
	 *
	 * Assumption: the list of points form a valid simple polygon
	 *
	 * @param points the list of points forming a valid simple polygon
	 *
	 * @return a {@link VPolygon} object of the list of points
	 */
	public static VPolygon polygonFromPoints2D(@NotNull final List<? extends IPoint> points) {
		Path2D.Double result = new Path2D.Double();
		if (points.isEmpty())
			return new VPolygon(result);

		IPoint last = points.get(points.size() - 1);
		result.moveTo(last.getX(), last.getY());

		for (int i = 0; i < points.size(); i++) {
			result.lineTo(points.get(i).getX(), points.get(i).getY());
		}

		return new VPolygon(result);
	}

	public static VPolygon polygonFromArea(@NotNull final Area area){
		VPolygon tmpPolygon = new VPolygon(area);
		return polygonFromPoints2D(tmpPolygon.getPoints());
	}

	/**
	 * Constructs a new Polygon defined by the vertices. It is assumed that
	 * all vertices are distinct.
	 *
	 * @param vertices the defining distinct vertices.
	 *
	 * @return a new Polygon
	 */
	public static VPolygon polygonFromPoints2D(@NotNull final IPoint... vertices) {
		Path2D.Double result = new Path2D.Double();
		if (vertices.length == 0)
			return new VPolygon(result);

		IPoint last = vertices[vertices.length - 1];
		result.moveTo(last.getX(), last.getY());

		for (int i = 0; i < vertices.length; i++) {
			result.lineTo(vertices[i].getX(), vertices[i].getY());
		}

		return new VPolygon(result);
	}

	/**
	 * Computes the area of a Polygon.
	 *
	 * @param vertices distinct vertices defining the polygon.
	 *
	 * @return the area of a polygon
	 */
	public static double signedAreaOfPolygon(@NotNull final List<? extends IPoint> vertices) {
		double result = 0;
		if(vertices.size() >= 3) {
			for (int i = 0; i < vertices.size() - 1; i++) {
				result += vertices.get(i).getX() * vertices.get(i + 1).getY() - vertices.get(i + 1).getX() * vertices.get(i).getY();
			}
			int n = vertices.size() - 1;
			result += vertices.get(n).getX() * vertices.get(0).getY() - vertices.get(0).getX() * vertices.get(n).getY();
		}
		return result / 2.0;
	}

	public static double signedAreaOfPolygon(@NotNull final double x[], @NotNull final double y[]) {
		assert x.length == y.length;
		double result = 0;
		if(x.length >= 3) {
			for (int i = 0; i < x.length - 1; i++) {
				result += x[i] * y[i + 1] - x[i + 1] * y[i];
			}
			int n = x.length - 1;
			result += x[n] * y[0] - x[0] * y[n];
		}
		return result / 2.0;
	}

	public static double signedAreaOfPolygon(@NotNull final IPoint... vertices) {
		double result = 0;
		if(vertices.length >= 3) {
			for (int i = 0; i < vertices.length - 1; i++) {
				result += vertices[i].getX() * vertices[i + 1].getY() - vertices[i + 1].getX() * vertices[i].getY();
			}
			int n = vertices.length - 1;
			result += vertices[n].getX() * vertices[0].getY() - vertices[0].getX() * vertices[n].getY();
		}
		return result / 2.0;
	}

	public static double areaOfTriangle(@NotNull final double x1, double y1, double x2, double y2, double x3, double y3){
		return Math.abs(x1 * (y2 - y3) - x2 * (y1 - y3) + x3 * (y1 - y2));
	}


	public static double areaOfPolygon(@NotNull final double x[], @NotNull final double y[]){
		return Math.abs(signedAreaOfPolygon(x, y));
	}

	public static double areaOfPolygon(@NotNull final double x[], @NotNull final double y[], @NotNull final double z[]){
		assert x.length >= 3 && x.length == y.length && x.length == z.length;

		double[] total = new double[]{0, 0, 0};

		for(int i = 0; i < x.length; i++) {
			double[] vi1 = new double[] {x[i], y[i], z[i]};
			double[] vi2;

			if(i == x.length - 1) {
				vi2 = new double[] {x[0], y[0], z[0]};
			} else {
				vi2 = new double[] {x[i+1], y[i+1], z[i+1]};
			}

			double[] prod = new double[3];
			cross(vi1, vi2, prod);
			total[0] += prod[0];
			total[1] += prod[1];
			total[2] += prod[2];
		}

		double result = dot(total, unitNorm(new double[]{x[0], y[0], z[0]}, new double[]{x[1], y[1], z[1]}, new double[]{x[2], y[2], z[2]}));
		return Math.abs(result/2.0);
	}

	public static double areaOfPolygon(@NotNull final List<? extends IPoint> vertices){
		return Math.abs(signedAreaOfPolygon(vertices));
	}

	public static double areaOfPolygon(@NotNull final IPoint... vertices){
		return Math.abs(signedAreaOfPolygon(vertices));
	}

	/**
	 * Computes the distance from the line-segment defined by (p1,p2) to the point p.
	 *
	 * @param p1    first point of the line-segment
	 * @param p2    second point of the line-segment
	 * @param p     the point
	 *
	 * @return the distance from the line-segment defined by (p1,p2) to the point p.
	 */
	public static double distanceToLineSegment(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final IPoint p) {
		return distanceToLineSegment(p1, p2, p.getX(), p.getY());
	}

	/**
	 * Computes the distance from the line-segment defined by (p1,p2) to the point p = (x,y).
	 *
	 * @param p1    first point of the line-segment
	 * @param p2    second point of the line-segment
	 * @param x     x-coordinate of the point p
	 * @param y     y-coordinate of the point p
	 *
	 * @return the distance from the line-segment defined by (p1,p2) to the point p.
	 */
	public static double distanceToLineSegment(@NotNull final IPoint p1, @NotNull final IPoint p2, final double x, final double y) {
		return distanceToLineSegment(p1.getX(), p1.getY(), p2.getX(), p2.getY(), x, y);
	}

	/**
	 * Computes the Euclidean distance between p = (px, py) and q = (qx, qy).
	 *
	 * @param px    x-coordinate of p
	 * @param py    y-coordinate of p
	 * @param qx    x-coordinate of q
	 * @param qy    y-coordinate of q
	 *
	 * @return the Euclidean distance between p = (px, py) and q = (qx, qy)
	 */
	public static double distance(final double px, final double py, final double qx, final double qy) {
		return Math.sqrt((px - qx) * (px - qx) + (py - qy) * (py - qy));
	}

	/**
	 * Computes the distance between the line-segment (p1, p2) and the point (x, y).
	 *
	 * @param p1X   x-coordinate of p1
	 * @param p1Y   y-coordinate of p1
	 * @param p2X   x-coordinate of p2
	 * @param p2Y   y-coordinate of p2
	 * @param x     x-coordinate of the point of interest
	 * @param y     y-coordinate of the point of interest
	 *
	 * @return the distance between the line-segment (p1, p2) and the point (x, y)
	 */
	public static double distanceToLineSegment(final double p1X, final double p1Y, final double p2X, final double p2Y, final double x, final double y) {
		// special cases
		/*if(p1X == p2X) {
			if((y > p1Y && p1Y > p2Y) || (y < p1Y && p1Y < p2Y)) {
				return distance(p1X, p1Y, x, y);
			}
			else if((y > p2Y && p2Y > p1Y) || (y < p2Y && p2Y < p1Y)) {
				return distance(p2X, p2Y, x, y);
			}
			else {
				return Math.abs(p1X - x);
			}
		}

		if(p1Y == p2Y) {

			if((x > p1X && p1X > p2X) || (x < p1X && p1X < p2X)) {
				return distance(p1X, p1Y, x, y);
			}
			else if((x > p2X && p2X > p1X) || (x < p2X && p2X < p1X)) {
				return distance(p2X, p2Y, x, y);
			}
			else {
				return Math.abs(p1Y - y);
			}
		}*/

		double len2 = (p2X - p1X) * (p2X - p1X) + (p2Y - p1Y) * (p2Y - p1Y);
		double r = ((x - p1X) * (p2X - p1X) + (y - p1Y) * (p2Y - p1Y)) / len2;

		if (r <= 0.0)
			return GeometryUtils.distance(p1X, p1Y, x, y);
		if (r >= 1.0)
			return GeometryUtils.distance(p2X, p2Y, x, y);

		double s = ((p1Y - y) * (p2X - p1X) - (p1X - x) * (p2Y - p1Y)) / len2;
		return Math.abs(s) * Math.sqrt(len2);
	}

	/**
	 * Computes the distance between the line defined by (p1, p2) and the point (x, y).
	 *
	 * @param p1    first point of the line
	 * @param p2    second point of the line
	 * @param x     x-coordinate of the point of interest
	 * @param y     y-coordinate of the point of interest
	 *
	 * @return the distance between the line defined by (p1, p2) and the point (x, y).
	 */
	public static double distanceToLine(@NotNull final IPoint p1, @NotNull final IPoint p2, final double x, final double y) {
		return distanceToLine(p1.getX(), p1.getY(), p2.getX(), p2.getY(), x, y);
	}

	/**
	 * Computes the distance between the line defined by (p1, p2) and the point (x, y).
	 *
	 * @param p1X   x-coordinate of p1
	 * @param p1Y   y-coordinate of p1
	 * @param p2X   x-coordinate of p2
	 * @param p2Y   y-coordinate of p2
	 * @param x     x-coordinate of the point of interest
	 * @param y     y-coordinate of the point of interest
	 *
	 * @return the distance between the line defined by (p1, p2) and the point (x, y)
	 */
	public static double distanceToLine(final double p1X, final double p1Y, final double p2X, final double p2Y, final double x, final double y) {
		double a = p1Y - p2Y;
		double b = p2X - p1X;
		double c = p1X * p2Y - p2X * p1Y;
		if(a == 0 && b == 0) {
			return 0.0;
		}
		else {
			return Math.abs(a * x + b * y + c) / Math.sqrt(a * a + b * b);
		}
	}

	/**
	 * Tests if the point p is on the line-segment (p1, p2) (or very very close).
	 *
	 * @param p1        the first point of the line-segment
	 * @param p2        the second point of the line-segment
	 * @param p         the point of interest
	 * @param tolerance if the point is closer than this tolerance it is assumed that it is on it
	 *
	 * @return if the point p is on the line-segment (p1, p2) (or very very close), false otherwise
	 */
	public static boolean isOnEdge(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final IPoint p, double tolerance) {
		return distanceToLineSegment(p1, p2, p) < tolerance;
	}

	/**
	 * Computes the intersection points of a line and a circle. The line is supposed to have infinity
	 * length and is defined by the two points of the VLine.
	 *
	 * @param line      the line
	 * @param circle    the circle
	 *
	 * @return  all intersection poins of the line with the circle i.e. 1, 2 or 0 results.
	 */
	public static VPoint[] intersection(@NotNull final VLine line, @NotNull final VCircle circle) {
		double m = line.slope();
		double d = line.getY1() - m * line.getX1();
		double a = circle.getCenter().getX();
		double b = circle.getCenter().getY();

		double discreminant = circle.getRadius() * circle.getRadius() * (1 + m*m) - (b -m * a -d) * (b - m * a - d);


		if(discreminant < 0) {
			return new VPoint[0];
		}
		else if(discreminant == 0){
			double x = (a + b * m - d * m) / (1 + m*m);
			double y = m * x + d;
			return  new VPoint[]{new VPoint(x, y)};
		}
		else {
			double x1 = (a + b * m - d * m + Math.sqrt(discreminant)) / (1 + m*m);
			double y1 = m * x1 + d;

			double x2 = (a + b * m - d * m - Math.sqrt(discreminant)) / (1 + m*m);
			double y2 = m * x2 + d;

			return new VPoint[]{ new VPoint(x1, y1), new VPoint(x2, y2)};
		}
	}

	/**
	 * The (smallest possible) angle3D at C from the triangle ACB.
	 *
	 * @param A first point of the triangle
	 * @param C second point of the triangle
	 * @param B third point of the triangle
	 *
	 * @return the (smallest possible) angle3D at C from the triangle ACB.
	 */
	public static double angle(@NotNull final IPoint A, @NotNull final IPoint C, @NotNull final IPoint B) {
		return angle(A.getX(), A.getY(), C.getX(), C.getY(), B.getX(), B.getY());
	}
	public static double angle(final double xa, final double ya, final double xc, final double yc, final double xb, final double yb) {
		double phi1 = angleTo(xa, ya, xc, yc);
		double phi2 = angleTo(xb, yb, xc, yc);
		double phi = Math.abs(phi1 - phi2);
		return Math.min(phi, 2 * Math.PI - phi);
	}

	public static double angle3D(
			final double ax, final double ay, final double az,
			final double bx, final double by, final double bz) {

		double dot = (ax * bx + ay * by + az * bz);
		double aLen = len(ax, ay, az);
		double bLen = len(bx, by, bz);
		double c = dot / (aLen * bLen);

		if(Math.abs(c - 1.0) < GeometryUtils.DOUBLE_EPS) {
			return 0;
		}
		return Math.acos(c);
	}

	public static double angle2D(
			final double ax, final double ay,
			final double bx, final double by) {

		double dot = (ax * bx + ay * by);
		double aLen = len(ax, ay);
		double bLen = len(bx, by);
		double c = dot / (aLen * bLen);

		if(Math.abs(c - 1.0) < GeometryUtils.DOUBLE_EPS) {
			return 0;
		}
		return Math.acos(c);
	}

	public static double len(final double ax, final double ay, final double az) {
		return Math.sqrt(ax * ax + ay * ay + az * az);
	}

	public static double len(final double ax, final double ay) {
		return Math.sqrt(ax * ax + ay * ay);
	}

	/**
	 * Computes the angle3D between the positive x-axis and the point (to - from).
	 * Result is in interval (0,2*PI) according to standard math usage.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Atan2">https://en.wikipedia.org/wiki/Atan2</a>
	 * @param from the first point / vector
	 * @param to   the second point / vector
	 *
	 * @return the angle3D between the positive x-axis and the vector (from -> to)
	 */
	public static double angleTo(@NotNull final IPoint from, @NotNull final IPoint to) {
		double atan2 = Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());

		if (atan2 < 0.0) {
			atan2 = Math.PI * 2 + atan2;
		}

		return atan2;
	}


	/**
	 * Computes the angle3D between the positive x-axis and the point (to - (0,0)).
	 * Result is in interval (0,2*PI) according to standard math usage.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Atan2">https://en.wikipedia.org/wiki/Atan2</a>
	 * @param to   the second point / vector
	 *
	 * @return the angle3D between the positive x-axis and the vector ((0,0) -> to)
	 */
	public static double angleTo(@NotNull final IPoint to) {
		return angleTo(new VPoint(0,0), to);
	}

	/**
	 * Returns the angle3D between two lines in clock wise order (cw).
	 *
	 * @param line1 the first line
	 * @param line2 the second line
	 *
	 * @return the angle3D between two lines in clock wise order (cw).
	 */
	public static double angleBetween2Lines(@NotNull final VLine line1, @NotNull final VLine line2)
	{
		double angle1 = Math.atan2(line1.getY1() - line1.getY2(),
				line1.getX1() - line1.getX2());
		double angle2 = Math.atan2(line2.getY1() - line2.getY2(),
				line2.getX1() - line2.getX2());
		return (angle1-angle2) < 0 ? (angle1-angle2) + 2*Math.PI :(angle1-angle2);
	}

	/**
	 * Computes a {@link VRectangle} square which is the tight bounding box of the collection of points.
	 *
	 * @param points    collection of points
	 * @param <P>       the type of the {@link IPoint}
	 *
	 * @return a square which is the tight bounding box of the collection of points
	 */
	public static <P extends IPoint> VRectangle bound(final Collection<P> points) {
		return bound(points, 0.0);
	}

	/**
	 * Computes a {@link VRectangle} square which is the tight bounding box of the collection of points.
	 * The bounding box is padded by <tt>padding</tt>
	 *
	 * @param points    collection of points
	 * @param padding   the padding
	 * @param <P>       the type of the {@link IPoint}
	 *
	 * @return a square padded by <tt>padding</tt> which is the tight bounding box of the collection of points
	 */
	public static <P extends IPoint> VRectangle bound(final Collection<P> points, final double padding) {
		if(points.isEmpty()) {
			throw new IllegalArgumentException("the point collection is empty.");
		}

		VPoint pMax = points.stream().map(p -> new VPoint(p.getX(), p.getY())).reduce((p1, p2) -> new VPoint(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()))).get();
		VPoint pMin = points.stream().map(p -> new VPoint(p.getX(), p.getY())).reduce((p1, p2) -> new VPoint(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()))).get();

		return new VRectangle(pMin.getX()-padding, pMin.getY()-padding, pMax.getX() - pMin.getX() + 2*padding, pMax.getY() - pMin.getY() + 2*padding);
	}

	public static <P extends IPoint> VRectangle boundRelative(final Collection<P> points, final double persentage) {
		if(points.isEmpty()) {
			throw new IllegalArgumentException("the point collection is empty.");
		}

		VPoint pMax = points.stream().map(p -> new VPoint(p.getX(), p.getY())).reduce((p1, p2) -> new VPoint(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()))).get();
		VPoint pMin = points.stream().map(p -> new VPoint(p.getX(), p.getY())).reduce((p1, p2) -> new VPoint(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()))).get();

		double padding = Math.max(pMax.x - pMin.x, pMax.y - pMin.y) * persentage;

		return new VRectangle(pMin.getX()-padding, pMin.getY()-padding, pMax.getX() - pMin.getX() + 2*padding, pMax.getY() - pMin.getY() + 2*padding);
	}

	public static <P extends IPoint> VRectangle boundRelativeSquared(final Collection<P> points, final double persentage) {
		if(points.isEmpty()) {
			throw new IllegalArgumentException("the point collection is empty.");
		}

		VPoint pMax = points.stream().map(p -> new VPoint(p.getX(), p.getY())).reduce((p1, p2) -> new VPoint(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()))).get();
		VPoint pMin = points.stream().map(p -> new VPoint(p.getX(), p.getY())).reduce((p1, p2) -> new VPoint(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()))).get();

		double max = Math.max(pMax.x - pMin.x, pMax.y - pMin.y);

		double padding = max * persentage;

		return new VRectangle(pMin.getX()-padding, pMin.getY()-padding, pMin.getX() + max + 2*padding, pMin.getY() + max + 2*padding);
	}

	public static <P extends IPoint> VRectangle boundRelative(final Collection<P> points) {
		return boundRelative(points, 0.01);
	}

	/**
	 *  This method divides an non-acute triangle ACB into 7 acute triangles
	 *  <ul>
	 *      <li>AFE</li>
	 *      <li>EFD</li>
	 *      <li>DCE</li>
	 *      <li>DHC</li>
	 *      <li>DGH</li>
	 *      <li>FGD</li>
	 *      <li>GBH</li>
	 *  </ul>
	 *  If the triangle is non-acute at C. If the triangle is already acute the method returns the original triangle.
	 *  throws an illegal argument exception if the triangle is not a feasible triangle.
	 *
	 * @see <a href="https://proofwiki.org/wiki/Obtuse_Triangle_Divided_into_Acute_Triangles">https://proofwiki.org/wiki/Obtuse_Triangle_Divided_into_Acute_Triangles</a>
	 * @param triangle the triangle which might be divided into 7 acute triangles
	 * @return if the triangle is acute this method returns it, otherwise it returns 7 acute triangles
	 */
	public static VTriangle[] generateAcuteTriangles(final VTriangle triangle) {
		double angle1 = angle(triangle.p1, triangle.p2, triangle.p3);
		double angle2 = angle(triangle.p2, triangle.p3, triangle.p1);
		double angle3 = angle(triangle.p3, triangle.p1, triangle.p2);
		double tolerance = 0.000001;

		// non-acute triangle
		if(triangle.isNonAcute()) {
			VPoint c;
			VPoint a;
			VPoint b;
			if(angle1 > angle2 && angle1 > angle3) {
				a = triangle.p3;
				c = triangle.p2;
				b = triangle.p1;
			}
			else if(angle2 > angle1 && angle2 > angle3) {
				a = triangle.p1;
				c = triangle.p3;
				b = triangle.p2;
			}
			else if(angle3 > angle1 && angle3 > angle2) {
				a = triangle.p2;
				c = triangle.p1;
				b = triangle.p3;
			}
			else {
				throw new IllegalArgumentException(triangle + " is not a feasible triangle");
			}

			VPoint d = triangle.getIncenter();
			VCircle circle = new VCircle(d, d.distance(c));
			VPoint[] iPoints = intersection(new VLine(a, c), circle);

			VPoint e = iPoints[0].equals(c, tolerance) ? iPoints[1] : iPoints[0];
			iPoints = intersection(new VLine(b, c), circle);
			VPoint h = iPoints[0].equals(c, tolerance) ? iPoints[1] : iPoints[0];

			iPoints = intersection(new VLine(a, b), circle);
			VPoint f = iPoints[0].distance(a) < iPoints[1].distance(a) ? iPoints[0] : iPoints[1];
			VPoint g = iPoints[0].distance(a) < iPoints[1].distance(a) ? iPoints[1] : iPoints[0];

			return new VTriangle[]{
					new VTriangle(a, f, e),
					new VTriangle(e, f, d),
					new VTriangle(d, c, e),
					new VTriangle(d, h ,c),
					new VTriangle(d, g, h),
					new VTriangle(f, g, d),
					new VTriangle(g, b, h)
			};
		}
		else {
			return new VTriangle[]{triangle};
		}

	}

	/**
	 * Returns the {@link VPoint} p1+p2.
	 *
	 * @param p1 first point
	 * @param p2 second point
	 *
	 * @return p1+p2
	 */
	public static VPoint add(final VPoint p1, final VPoint p2) {
		return new VPoint(p1.x + p2.x, p1.y + p2.y);
	}

	/**
	 * Computes the quality smaller or equals one of the triangle with respect to the length of the edges,
	 * i.e. a triangle where each edge has equal length has the quality 1.
	 *
	 * @param triangle the triangle
	 *
	 * @return the quality smaller or equals one of the triangle
	 */
	public static double qualityOf(@NotNull final VTriangle triangle) {
		return GeometryUtils.qualityInCircleOutCircle(triangle.p1, triangle.p2, triangle.p3);
	}

	/**
	 * Computes the intersection point of two lines ((x1, y1), (x2, y2)) and ((x3, y3), (x4, y4)).
	 *
	 * Assumption: the lines are not co-linear.
	 *
	 * @param x1 x-coordinate of the first point of the first line
	 * @param y1 y-coordinate of the first point of the first line
	 * @param x2 x-coordinate of the second point of the first line
	 * @param y2 y-coordinate of the second point of the first line
	 * @param x3 x-coordinate of the first point of the second line
	 * @param y3 y-coordinate of the first point of the second line
	 * @param x4 x-coordinate of the second point of the second line
	 * @param y4 y-coordinate of the second point of the second line
	 *
	 * @return the intersection point of the two lines
	 */
	public static VPoint lineIntersectionPoint(final double x1,
											   final double y1,
											   final double x2,
											   final double y2,
											   final double x3,
											   final double y3,
											   final double x4,
											   final double y4) {
		assert new VLine(new VPoint(x1, y1), new VPoint(x2, y2)).intersectsLine(x3, y3, x4, y4);
		return intersectionPoint(x1, y1, x2, y2, x3, y3, x4, y4);
		/*double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
		assert d != 0;

		double x = ((x1 * y2 - y1 - x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
		double y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (y3 * y4 - y3 * x4)) / d;
		return new VPoint(x, y);*/
	}

	/**
	 * Computes the intersection point of two lines (p1, p2) and (q1, q2).
	 *
	 * Assumption: the lines are not co-linear.
	 *
	 * @param p1 first point of the first line
	 * @param p2 second point of the first line
	 * @param q1 first point of the second line
	 * @param q2 second point of the second line
	 *
	 * @return the intersection point of the two lines
	 */
	public static VPoint lineIntersectionPoint(final VPoint p1, final VPoint p2, final VPoint q1, final VPoint q2) {
		return lineIntersectionPoint(p1.x, p1.y, p2.x, p2.y, q1.x, q1.y, q2.x, q2.y);
	}

	/**
	 * Computes the intersection point of two <tt>line</tt> and (q1, q2).
	 *
	 * Assumption: the lines are not co-linear.
	 *
	 * @param line  the first line
	 * @param x3    x-coordinate of the first point of the second line
	 * @param y3    y-coordinate of the first point of the second line
	 * @param x4    x-coordinate of the second point of the second line
	 * @param y4    y-coordinate of the second point of the second line
	 *
	 * @return the intersection point of the two lines
	 */
	public static VPoint lineIntersectionPoint(final VLine line,
											   final double x3,
											   final double y3,
											   final double x4,
											   final double y4) {
		return lineIntersectionPoint(line.getX1(), line.getY1(), line.getX2(), line.getY2(), x3, y3, x4, y4);
	}

	/**
	 * Computes the intersection point of two <tt>line1</tt> and <tt>line2</tt>.
	 *
	 * Assumption: the lines are not co-linear.
	 *
	 * @param line1 the first line
	 * @param line2 the second line
	 *
	 * @return the intersection point of the two lines
	 */
	public static VPoint lineIntersectionPoint(final VLine line1,
	                                           final VLine line2) {
		return lineIntersectionPoint(line1.getX1(), line1.getY1(), line1.getX2(), line1.getY2(), line2.getX1(), line2.getY1(), line2.getX2(), line2.getY2());
	}

	/**
	 * <p>A brute force method to get the set of all intersection points of a list of shapes.
	 * For two shapes this requires O(n * m) time where n, m are the number of points of the shapes.</p>
	 *
	 * <p>Note: A sweepline algorithm could improve the performance significantly.</p>
	 *
	 * @param shapes a list of shapes
	 * @return a set of intersection points
	 */
	public static Set<VPoint> getIntersectionPoints(@NotNull final List<? extends VShape> shapes) {
		Set<VPoint> intersectionPoints = new HashSet<>();
		for(int i = 0; i < shapes.size(); i++) {
			for(int j = i + 1; j < shapes.size(); j++) {
				List<VPoint> path1 = shapes.get(i).getPath();
				List<VPoint> path2 = shapes.get(j).getPath();

				for(int ii = 0; ii < path1.size(); ii++) {
					VPoint p1 = path1.get((ii) % path1.size());
					VPoint p2 = path1.get((ii + 1) % path1.size());

					for(int jj = 0; jj < path2.size(); jj++) {
						VPoint q1 = path2.get((jj) % path2.size());
						VPoint q2 = path2.get((jj + 1) % path2.size());

						if(intersectLineSegment(p1, p2, q1, q2)) {
							VPoint intersectionPoint = GeometryUtils.lineIntersectionPoint(p1, p2, q1, q2);
							intersectionPoints.add(intersectionPoint);
						}
					}

				}
			}
		}

		return intersectionPoints;
	}

	/**
	 * <p>Transforms a list of distinct points (p1,p2,p3,...,pn) into a polygon.</p>
	 *
	 * <p>Assumption: the points are in the correct order i.e. ccw or cw. and the list contains
	 * more than 2 points.</p>
	 *
	 * @param points a list of points in order
	 * @return a polygon which is constructed via a list of points
	 */
	public static VPolygon toPolygon(@NotNull final List<? extends IPoint> points) {
		assert points.size() >= 3;
		if(points.size() < 3) {
			throw new IllegalArgumentException("more than 2 points are required to form a valid polygon.");
		}
		return polygonFromPoints2D(points);
	}

	public static VPolygon toPolygon(final VCircle circle, final int nPoints) {
		double alpha = 2 * Math.PI / nPoints;
		VPoint p = new VPoint(0, circle.getRadius());

		Path2D.Double path = new Path2D.Double();
		VPoint center = circle.getCenter();

		path.moveTo(center.x + p.x, center.y + p.y);
		for(int i = 1; i < nPoints; i++) {
			p = p.rotate(alpha);
			path.lineTo(center.x + p.x, center.y + p.y);
		}
		return new VPolygon(path);
	}

	/**
	 * Tests if two polygons are equals, i.e. they are defined by the same list of points.
	 * The list of the first polygon might be shifted and/or reversed with respect to the second polygon.
	 *
	 * @param poly1 the first polygon
	 * @param poly2 the second polygon
	 * @return true if both polygons are defined by the same path, false otherwise
	 */
	public static boolean equalsPolygons(@NotNull final VPolygon poly1, @NotNull final VPolygon poly2) {
		return equalsPolygonsInOrder(poly1, poly2) || equalsPolygonsInOrder(poly1.revertOrder(), poly2);
	}

	/**
	 * Tests if two polygons are equals, i.e. they are defined by the same list of points.
	 * The list of the first polygon might be shifted with respect to the second polygon.
	 *
	 * @param poly1 the first polygon
	 * @param poly2 the second polygon
	 * @return true if both polygons are defined by the same path, false otherwise
	 */
	private static boolean equalsPolygonsInOrder(@NotNull final VPolygon poly1, @NotNull final VPolygon poly2) {
		List<VPoint> pointList1 = poly1.getPoints();
		List<VPoint> pointList2 = poly2.getPoints();

		if(pointList1.size() != pointList2.size()) {
			return false;
		}

		if(pointList1.isEmpty() && pointList2.isEmpty()) {
			return true;
		}

		boolean found = false;
		int j = -1;
		for(int i = 0; i < pointList1.size(); i++) {
			VPoint p0 = pointList2.get(0);
			if(p0.equals(pointList1.get(i))) {
				j = i;
				found = true;
				break;
			}
		}

		if(!found) {
			return false;
		}

		for(int i = 0; i < pointList2.size(); i++) {
			if(!pointList2.get(i).equals(pointList1.get((j+i) % pointList1.size()))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * <p>This method removes duplicated and co-linear points from a list (p1, ..., pn) of points which form a simple polygon.
	 * points p1, p2, p3 are seen as co-linear if the distance from p3 to line p1 to p2 is smaller or equals eps.</p>
	 *
	 * <p>Assumption: p1 to ... to pn forms a simple polygon.</p>
	 *
	 * @param points    points of a polygon.
	 * @param eps       the distance which determines if three points are co-linear
	 *
	 * @return a list of points forming a simple polygon such that there are no duplicated or co-linear points.
	 */
	public static List<VPoint> filterUselessPoints(@NotNull final List<VPoint> points, final double eps) {
		assert points.size() >= 3;
		List<VPoint> filteredList = new ArrayList<>(points);

		boolean removePoint;

		do {
			removePoint = false;
			for(int i = 0; i < filteredList.size(); i++) {

				VPoint p1 = filteredList.get((i + filteredList.size()-1) % filteredList.size());
				VPoint p2 = filteredList.get(i);
				VPoint p3 = filteredList.get((i + 1) % filteredList.size());

				if(p2.equals(p1) || p2.equals(p3) || GeometryUtils.distanceToLineSegment(p1, p3, p2) <= eps) {
					filteredList.remove(i);
					removePoint = true;
					break;
				}
			}
		} while (removePoint);

		return filteredList;
	}

	/**
	 * Transforms a list of points into a simple polygon {@link VPolygon}.
	 *
	 * @param sorted if true the method assumes that the list of points is sorted i.e. forms already a valid simple polygon,
	 *               otherwise the list will be sorted in ccw order beforehand.
	 * @param points the list of points
	 * @return a simple polygon defined by the list of points
	 */
	public static VPolygon toPolygon(final boolean sorted, @NotNull final List<IPoint> points) {
		assert points.size() >= 3;
		return toPolygon(sorted ? points : sortCCW(points));
	}

	/**
	 * Sorts a list of points in ccw order with respect to the centroid of the list of points.
	 *
	 * @param points the list of points
	 * @return a ccw sorted (with respect to the centroid of the points) list of the same points
	 */
	public static List<IPoint> sortCCW(@NotNull final List<IPoint> points) {
		List<IPoint> sortedPoints = new ArrayList<>(points);
		VPoint center = getCentroid(points);
		Collections.sort(sortedPoints, new CCWComparator(center));
		return sortedPoints;
	}

	/**
	 * Computes the centroid of an (unsorted) point set.
	 *
	 * @param points the point set (as list)
	 * @return the centroid of an (unsorted) point set
	 */
	public static VPoint getCentroid(@NotNull final List<IPoint> points) {
		double x = 0;
		double y = 0;
		for(IPoint p : points) {
			x += p.getX();
			y += p.getY();
		}
		return new VPoint(x / points.size(), y / points.size());
	}

	/**
	 * <p>Transforms a list of distinct points (p1,p2,p3,...,pn) into a polygon.</p>
	 *
	 * <p>Assumption: the points are in the correct order i.e. ccw or cw. and the list contains
	 * more than 2 points.</p>
	 *
	 * @param points an array / list of points in order
	 * @return a polygon
	 */
	public static VPolygon toPolygon(@NotNull final IPoint ... points) {
		assert points.length >= 3;
		if(points.length < 3) {
			throw new IllegalArgumentException("more than 2 points are required to form a valid polygon.");
		}

		Path2D path2D = new Path2D.Double();
		path2D.moveTo(points[0].getX(), points[0].getY());
		//path2D.lineTo(points[0].getX(), points[0].getY());

		for(int i = 1; i < points.length; i++) {
			path2D.lineTo(points[i].getX(), points[i].getY());
		}

		path2D.lineTo(points[0].getX(), points[0].getY());

		return new VPolygon(path2D);
	}

	/**
	 * <p>Returns a point which lies inside the polygon</p>
	 *
	 * Assumption: the polygon is a valid simple polygon!
	 *
	 * @param polygon
	 * @return a point which lies inside the polygon
	 */
	public static VPoint getInteriorPoint(@NotNull final VPolygon polygon) {
		// for a convex polygon the centroid is a valid point
		if(polygon.contains(polygon.getCentroid())) {
			return polygon.getCentroid();
		}

		int maxTrys = 31; // for 32 bit integer
		double factor = 100;
		double minLineLength = polygon.getLinePath().stream().map(line -> line.length()).min(Double::compareTo).get();
		Rectangle2D rectangle2D = polygon.getBounds2D();

		for(int i = 1; i < maxTrys; i++){
			double eps = Math.min(Math.min(rectangle2D.getWidth(), rectangle2D.getHeight()), minLineLength) / (factor * ((1 << i)-1));
			for(double x = rectangle2D.getX()+eps; x < rectangle2D.getX()+rectangle2D.getWidth(); x+=eps) {
				for(double y = rectangle2D.getY()+eps; y < rectangle2D.getY()+rectangle2D.getHeight(); y+=eps) {
					VPoint p = new VPoint(x, y);
					if(polygon.contains(p)) {
						return p;
					}
				}
			}
		}

		throw new IllegalArgumentException("unable to find an interior point for the polygon " + polygon);
	}

	/**
	 * Computes the projection of a onto b.
	 * See: https://en.wikipedia.org/wiki/Vector_projection
	 *
	 * @param ax x-coordinate of a
	 * @param ay y-coordinate of a
	 * @param bx x-coordinate of b
	 * @param by y-coordinate of b
	 * @return the projection of a onto b
	 */
	public static VPoint projectOnto(double ax, double ay, double bx, double by) {
		assert bx * bx + by * by > GeometryUtils.DOUBLE_EPS;
		double blen = Math.sqrt(bx * bx + by * by);
		double bxn = bx / blen;
		double byn = by / blen;

		// scalar product
		double alpha = ax * bxn + ay * byn;
		VPoint a1 = new VPoint(bxn * alpha, byn * alpha);
		return a1;
	}

	/**
	 * Projects the point (qx, qy) onto the line defined by (p0 = (p0x, p0y), p1 = (p1x, p1y)).
	 * see https://cs.nyu.edu/~yap/classes/visual/03s/hw/h2/math.pdf
	 *
	 * @param qx x-coordinate of a
	 * @param qy y-coordinate of a
	 * @param p0x x-coordinate of p0
	 * @param p0y y-coordinate of p0
	 * @param p1x x-coordinate of p1
	 * @param p1y y-coordinate of p1
	 *
	 * @return he projection of a onto the line (p,q)
	 */
	public static VPoint projectOntoLine(double qx, double qy, double p0x, double p0y, double p1x, double p1y) {
		double ax11 = p1x - p0x;
		double ax12 = p1y - p0y;
		double ax21 = p0y - p1y;
		double ax22 = p1x - p0x;
		double bx1 = qx * (p1x - p0x) + qy * (p1y - p0y);
		double bx2 = p0y * (p1x - p0x) - p0x * (p1y - p0y);
		double det = (ax11 * ax22 - ax12 * ax21);

		double px1 = (ax22 * bx1 - ax12 * bx2) / det;
		double px2 = (-ax21 * bx1 + ax11 * bx2) / det;
		return new VPoint(px1, px2);
	}
}
