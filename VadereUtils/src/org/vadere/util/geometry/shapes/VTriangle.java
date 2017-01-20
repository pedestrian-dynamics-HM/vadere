package org.vadere.util.geometry.shapes;

import java.util.Arrays;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.omg.CORBA.portable.ValueOutputStream;
import org.vadere.util.geometry.DataPoint;
import org.vadere.util.geometry.GeometryUtils;

/**
 * A triangle. Points must be given in counter clockwise manner to get correct
 * inward facing normals.
 * 
 */
public class VTriangle extends VPolygon {

	/**
	 * generated serial version uid
	 */
	private static final long serialVersionUID = 5864412321949258915L;

	public final VPoint p1;
	public final VPoint p2;
	public final VPoint p3;

	public final VLine[] lines;

	/**
	 * The centroid will be saved for performance boost, since this object is immutable.
	 */
	private VPoint centroid;

	private VPoint center;

	private VPoint incenter;

	private VPoint orthocenter;

	/**
	 * Creates a triangle. Points must be given in ccw order.
	 * 
	 * @param p1
	 * @param p2
	 * @param p3
	 */
	public VTriangle(@NotNull VPoint p1, @NotNull VPoint p2, @NotNull VPoint p3) {
		super(GeometryUtils.polygonFromPoints2D(p1, p2, p3));

		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;

		lines = new VLine[]{ new VLine(p1, p2), new VLine(p2, p3), new VLine(p3,p1) };
	}

	public VPoint midPoint() {
		return new VPoint((p1.x + p2.x + p3.x) / 3.0,
				(p1.y + p2.y + p3.y) / 3.0);
	}

	public boolean isLine() {
		VLine l1 = new VLine(p1, p2);
		VLine l2 = new VLine(p1, p3);
		VLine l3 = new VLine(p2, p3);

		return l1.ptSegDist(p3) < GeometryUtils.DOUBLE_EPS
				|| l2.ptSegDist(p2) < GeometryUtils.DOUBLE_EPS
				|| l3.ptSegDist(p1) < GeometryUtils.DOUBLE_EPS;
	}

	public boolean isNonAcute() {
		double angle1 = GeometryUtils.angle(p1, p2, p3);
		double angle2 = GeometryUtils.angle(p2, p3, p1);
		double angle3 = GeometryUtils.angle(p3, p1, p2);

		// non-acute triangle
		double maxAngle = Math.max(Math.max(angle1, angle2), angle3);
		double rightAngle = Math.PI/2;
		return maxAngle > rightAngle;
	}

	@Override
	public VPoint getCentroid() {
		if(centroid == null) {
			centroid = super.getCentroid();
		}
		return centroid;
	}

	public VPoint getIncenter(){
		if(incenter == null) {
			double a = p1.distance(p2);
			double b = p2.distance(p3);
			double c = p3.distance(p1);
			double perimeter = a + b + c;

			incenter = new VPoint((a * p3.x + b * p1.x + c * p2.x) / perimeter,
					(a * p3.y + b * p1.y + c * p2.y) / perimeter);
		}

		return incenter;
	}

	public VPoint getOrthocenter() {
		if(orthocenter == null) {
			double slope = -1 / ((p2.y - p1.y) / (p2.x - p1.x));
			// y  = slope * (x - p3.x) + p3.y

			double slope2 = -1 / ((p1.y - p3.y) / (p1.x - p3.x));
			// y = slope2 * (x - p2.x) + p2.y

			// slope2 * (x - p2.x) + p2.y  = slope * (x - p3.x) + p3.y
			// slope2 * (x - p2.x) - slope * (x - p3.x)  =  + p3.y - p2.y
			// slope2 * x - slope2 * p2.x - slope * x + slope * p3.x =  + p3.y - p2.y
			// slope2 * x  - slope * x  =  + p3.y - p2.y + slope2 * p2.x - slope * p3.x
			// x * (slope2 - slope) =  + p3.y - p2.y + slope2 * p2.x - slope * p3.x
			double x = (p3.y - p2.y + slope2 * p2.x - slope * p3.x) / (slope2 - slope);
			double y = slope * (x - p3.x) + p3.y;

			orthocenter = new VPoint(x, y);
		}

		return orthocenter;
	}

	public VPoint getCircumcenter(){
		if(center == null) {
			double d = 2 * (p1.getX() * (p2.getY() - p3.getY()) + p2.getX() * (p3.getY() - p1.getY()) + p3.getX() * (p1.getY() - p2.getY()));
			double x = ((p1.getX() * p1.getX() + p1.getY() * p1.getY()) * (p2.getY() - p3.getY())
					+ (p2.getX() * p2.getX() + p2.getY() * p2.getY()) * (p3.getY() - p1.getY())
					+ (p3.getX() * p3.getX() + p3.getY() * p3.getY()) * (p1.getY() - p2.getY())) / d;
			double y = ((p1.getX() * p1.getX() + p1.getY() * p1.getY()) * (p3.getX() - p2.getX())
					+ (p2.getX() * p2.getX() + p2.getY() * p2.getY()) * (p1.getX() - p3.getX())
					+ (p3.getX() * p3.getX() + p3.getY() * p3.getY()) * (p2.getX() - p1.getX())) / d;

			center = new VPoint(x,y);
		}

		return center;
	}

	public double getCircumscribedRadius() {
		return getCircumcenter().distance(p1);
	}

	public boolean isInCircumscribedCycle(final VPoint point) {
		return getCircumcenter().distance(point) < getCircumscribedRadius();
	}

	/**
	 * Computes the inward facing normal vector for the given points of the
	 * triangle.
	 * 
	 * @param p1
	 * @param p2
	 * @return inward facing normal vector
	 */
	public VPoint getNormal(final VPoint p1, final VPoint p2) {
		VPoint normal = new VPoint(p2.y - p1.y, -(p2.x - p1.x));
		// if the normal is already inward facing, return it
		if (GeometryUtils.ccw(p1, p2, normal) == GeometryUtils.ccw(p1, p2,
				this.midPoint())) {
			return normal;
		}
		// otherwise, reflect it
		else {
			return new VPoint(-normal.x, -normal.y);
		}
	}

	public boolean hasBoundaryPoint(final DataPoint point) {
		return this.p1.equals(point) || this.p2.equals(point)
				|| this.p3.equals(point);
	}

	public VLine[] getLines() {
		return lines;
	}

	public Stream<VLine> getLineStream() {
		return Arrays.stream(getLines());
	}

	@Override
	public String toString() {
		return p1 + "-" + p2 + "-" + p3;
	}
}
