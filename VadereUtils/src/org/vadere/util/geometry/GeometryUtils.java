package org.vadere.util.geometry;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.math.MathUtil;

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

	public static VPoint[] intersection(final VLine line, final VCircle circle) {
		VCircle normedCircle = new VCircle(new VPoint(0,0), circle.getRadius());
		Point2D p1 = line.getP1();
		Point2D p2 = line.getP2();
		VLine normedLine = new VLine(new VPoint(p1.getX(), p1.getY()).subtract(circle.getCenter()),
				new VPoint(p2.getX(), p2.getY()).subtract(circle.getCenter()));

		double dx = normedLine.getX2() - normedLine.getX1();
		double dy = normedLine.getY2() - normedLine.getY1();
		double drSquare = dx * dx + dy * dy;
		double dr = Math.sqrt(drSquare);
		double radius = normedCircle.getRadius();
		double determinant = normedLine.getX1() * normedLine.getY2() - normedLine.getX2() * normedLine.getY1();
		double discreminant = radius * radius * drSquare - determinant * determinant;

		if(discreminant < 0) {
			return new VPoint[0];
		}
		else if(discreminant == 0){
			return  new VPoint[]{
					new VPoint(determinant * dy / drSquare, -determinant * dx / drSquare).add(circle.getCenter())
			};
		}
		else {
			double sign = dy < 0 ? -1 : 1;
			double x1 = (determinant * dy + sign * dx * Math.sqrt(discreminant)) / drSquare;
			double y1 = (-determinant * dx + Math.abs(dy) * Math.sqrt(discreminant)) / drSquare;
			double x2 = (determinant * dy - sign * dx * Math.sqrt(discreminant)) / drSquare;
			double y2 = (-determinant * dx - Math.abs(dy) * Math.sqrt(discreminant)) / drSquare;

			return new VPoint[]{ new VPoint(x1, y1).add(circle.getCenter()), new VPoint(x2, y2).add(circle.getCenter())};
		}
	}

	public static VPoint[] intersection2(final VLine line, final VCircle circle) {
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
	 * The (smallest possible) angle at C from the triangle ACB.
	 *
	 * @param A
	 * @param C
	 * @param B
	 * @return
	 */
	public static double angle(VPoint A, VPoint C, VPoint B) {
		double phi1 = new Vector2D(A).angleTo(C);
		double phi2 = new Vector2D(B).angleTo(C);
		double phi = Math.abs(phi1 - phi2);
		return Math.min(phi, 2 * Math.PI - phi);
	}

	/**
	 * Returns the angle between line1 and line2 in clock wise order (cw).
	 * @param line1
	 * @param line2
	 * @return
	 */
	public static double angleBetween2Lines(final VLine line1, final VLine line2)
	{
		double angle1 = Math.atan2(line1.getY1() - line1.getY2(),
				line1.getX1() - line1.getX2());
		double angle2 = Math.atan2(line2.getY1() - line2.getY2(),
				line2.getX1() - line2.getX2());
		return (angle1-angle2) < 0 ? (angle1-angle2) + 2*Math.PI :(angle1-angle2);
	}


	/**
	 * This method follows the construction from
	 * https://proofwiki.org/wiki/Obtuse_Triangle_Divided_into_Acute_Triangles
	 * i.e. divides an non-acute triangle acb into 7 acute triangles:
	 *  new VTriangle(a, f, e),
	 *  new VTriangle(e, f, d),
	 *  new VTriangle(d, c, e),
	 *  new VTriangle(d, h ,c),
	 *  new VTriangle(d, g, h),
	 *  new VTriangle(f, g, d),
	 *  new VTriangle(g, b, h);.
	 *  if the triangle is non-acute at c. If the triangle is already acute the method
	 *  returns the original triangle.
	 *
	 * @param triangle
	 * @throws throws an illegal argument exception if the triangle is not a feasible triangle.
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
			VPoint[] iPoints = intersection2(new VLine(a, c), circle);

			VPoint e = iPoints[0].equals(c, tolerance) ? iPoints[1] : iPoints[0];
			iPoints = intersection2(new VLine(b, c), circle);
			VPoint h = iPoints[0].equals(c, tolerance) ? iPoints[1] : iPoints[0];

			iPoints = intersection2(new VLine(a, b), circle);

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

	public static VPoint add(VPoint p1, VPoint p2) {
		return new VPoint(p1.x + p2.x, p1.y + p2.y);
	}
}
