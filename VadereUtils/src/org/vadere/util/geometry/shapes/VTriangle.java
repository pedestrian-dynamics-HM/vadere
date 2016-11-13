package org.vadere.util.geometry.shapes;

import java.util.LinkedList;
import java.util.List;

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

	/**
	 * Creates a triangle. Points must be given in ccw order.
	 * 
	 * @param p1
	 * @param p2
	 * @param p3
	 */
	public VTriangle(VPoint p1, VPoint p2, VPoint p3) {
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

	@Override
	public VPoint getCentroid() {
		if(centroid == null) {
			centroid = super.getCentroid();
		}
		return centroid;
	}

	public VPoint getCenter(){
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
		return getCenter().distance(p1);
	}

	public boolean isInCircumscribedCycle(final VPoint point) {
		return getCenter().distance(point) < getCircumscribedRadius();
	}

	/**
	 * Computes the inward facing normal vector for the given points of the
	 * triangle.
	 * 
	 * @param p1
	 * @param p2
	 * @return inward facing normal vector
	 */
	public VPoint getNormal(VPoint p1, VPoint p2) {
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

	public boolean hasBoundaryPoint(DataPoint point) {
		return this.p1.equals(point) || this.p2.equals(point)
				|| this.p3.equals(point);
	}

	public VLine[] getLines() {
		return lines;
	}
}
