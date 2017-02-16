package org.vadere.util.geometry;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;

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
	public static VPoint closestToSegment(VLine line, IPoint point) {
		if (new VPoint((Point2D.Double) line.getP1()).equals(point)) {
			return new VPoint(line.x1, line.y1);
		}

		VPoint a2p = new VPoint(point.getX() - line.x1, point.getY() - line.y1);
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
			orderedList.add(new DataPoint(p.x, p.y, GeometryUtils.angleTo(p, center)));
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

	public static VPolygon polygonFromPoints2D(final List<VPoint> vertices) {
		return polygonFromPoints2D(vertices.toArray(new VPoint[0]));
	}

	/**
	 * Constructs a new Polygon defined by the vertices. It is assumed that
	 * all vertices a distinct.
	 *
	 * @param vertices the defining distinct vertices.
	 * @return a new Polygon
	 */
	public static VPolygon polygonFromPoints2D(final IPoint... vertices) {
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
	 * @return the area of a polygon
	 */
	public static double areaOfPolygon(final List<? extends IPoint> vertices) {
		double result = 0;
		if(vertices.size() >= 3) {
			for (int i = 0; i < vertices.size() - 1; i++) {
				result += vertices.get(i).getX() * vertices.get(i + 1).getY() - vertices.get(i + 1).getX() * vertices.get(i).getY();
			}
			int n = vertices.size() - 1;
			result += vertices.get(n).getX() * vertices.get(0).getY() - vertices.get(0).getX() * vertices.get(n).getY();
		}
		return Math.abs(result) / 2.0;
	}

	public static double areaOfPolygon(final IPoint... vertices) {
		double result = 0;

		for (int i = 0; i < vertices.length - 1; i++) {
			result += (vertices[i].getY() + vertices[i + 1].getY())
					* (vertices[i].getX() - vertices[i + 1].getX());
		}
		return Math.abs(result) / 2.0;
	}

	/**
	 * Computes the intersection points of a line and a circle. The line is supposed to have infinity
	 * length and is defined by the two points of the VLine.
	 *
	 * @param line      the line
	 * @param circle    the circle
	 * @return  all intersection poins of the line with the circle i.e. 1, 2 or 0 results.
	 */
	public static VPoint[] intersection(final VLine line, final VCircle circle) {
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
	public static double angle(IPoint A, IPoint C, IPoint B) {
		double phi1 = angleTo(A, C);
		double phi2 = angleTo(B, C);
		double phi = Math.abs(phi1 - phi2);
		return Math.min(phi, 2 * Math.PI - phi);
	}

	/**
	 *
	 * Computes the angle between the x-axis through the given Point "center" and this.
	 * Result is in interval (0,2*PI) according to standard math usage.
	 */
	public static double angleTo(final IPoint from, final IPoint to) {
		double atan2 = Math.atan2(from.getY() - to.getY(), from.getX() - to.getX());

		if (atan2 < 0.0) {
			atan2 = Math.PI * 2 + atan2;
		}

		return atan2;
	}

	public static double angleTo(final IPoint to) {
		return angleTo(new VPoint(0,0), to);
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

	public static double sign(final IPoint p1, final IPoint p2, final IPoint p3) {
		return (p1.getX() - p3.getX()) * (p2.getY() - p3.getY()) - (p2.getX() - p3.getX()) * (p1.getY() - p3.getY());
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

	public static VPoint add(final VPoint p1, final VPoint p2) {
		return new VPoint(p1.x + p2.x, p1.y + p2.y);
	}
}
