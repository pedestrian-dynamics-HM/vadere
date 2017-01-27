package org.vadere.util.geometry.shapes;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.vadere.util.geometry.ShapeType;
import org.vadere.util.geometry.Vector2D;

public class VCircle implements VShape {

	private final VPoint center;
	private final double radius;

	public VCircle(double x, double y, double radius) {
		this(new VPoint(x, y), radius);
	}

	public VCircle(VPoint center, double radius) {

		if (radius <= 0) {
			throw new IllegalArgumentException("Radius must be positive.");
		}

		this.center = center;
		this.radius = radius;
	}

	/**
	 * A circle at 0,0.
	 */
	public VCircle(double radius) {
		this(0, 0, radius);
	}

	public double getRadius() {
		return this.radius;
	}

	/**
	 * The distance to the boundary of the circle.
	 */
	@Override
	public double distance(IPoint pos) {
		return Math.abs(this.center.distance(pos) - this.radius);
	}

	public VPoint getCenter() {
		return this.center;
	}

	/**
	 * Not implemented.
	 */
	@Override
	public VPoint closestPoint(IPoint point) {
		Vector2D direction = new Vector2D(point.getX() - center.x, point.getY()
				- center.y);
		VPoint vector = direction.normalize(radius);
		return new VPoint(vector.x + center.x, vector.y + center.y);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof VCircle))
			return false;

		VCircle other = (VCircle) obj;

		if (this.radius != other.radius)
			return false;
		if (!this.center.equals(other.center))
			return false;

		return true;
	}

	@Override
	public Rectangle getBounds() {
		int diameter = (int) Math.ceil(2 * radius);
		return new Rectangle((int) Math.floor(center.x - radius),
				(int) Math.floor(center.y - radius), diameter, diameter);
	}

	@Override
	public Rectangle2D getBounds2D() {
		return new Rectangle2D.Double(center.x - radius, center.y - radius,
				2 * radius, 2 * radius);
	}

	@Override
	public boolean contains(double x, double y) {
		return Math.sqrt(Math.pow(center.x - x, 2) + Math.pow(center.y - y, 2)) <= radius;
	}

	@Override
	public boolean contains(Point2D p) {
		return center.distance(p) <= radius;
	}

	@Override
	public boolean contains(IPoint p) {
		return p.distance(center) <= radius;
	}

	@Override
	public boolean intersects(double x, double y, double w, double h) {
		VRectangle rect = new VRectangle(x, y, w, h);

		if (rect.distance(center) <= radius || rect.contains(center)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean intersects(Rectangle2D r) {
		return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	@Override
	public boolean contains(double x, double y, double w, double h) {
		return (contains(x, y) && contains(x + w, y) && contains(x, y + h) && contains(
				x + w, y + h));
	}

	@Override
	public boolean contains(Rectangle2D r) {
		return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/**
	 * Dummy implementation using Ellipse2D.Double.
	 */
	@Override
	public PathIterator getPathIterator(AffineTransform at) {
		return new Ellipse2D.Double(center.x - radius, center.y - radius,
				radius * 2, radius * 2).getPathIterator(at);
	}

	/**
	 * Dummy implementation using Ellipse2D.Double.
	 */
	@Override
	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		return new Ellipse2D.Double(center.x - radius, center.y - radius,
				radius * 2, radius * 2).getPathIterator(at, flatness);
	}

	@Override
	public VShape translate(final IPoint vector) {
		return new VCircle(getCenter().add(vector), getRadius());
	}

	@Override
	public VShape translatePrecise(final IPoint vector) {
		return new VCircle(getCenter().addPrecise(vector), getRadius());
	}

	@Override
	public VShape scale(final double scalar) {
		return new VCircle(getCenter().scalarMultiply(scalar), getRadius() * scalar);
	}

	@Override
	public boolean intersects(VLine intersectingLine) {
		if (intersectingLine.ptSegDist(this.getCenter()) <= this.getRadius())
			return true;
		return false;
	}

	@Override
	public VPoint getCentroid() {
		return center;
	}

	@Override
	public ShapeType getType() {
		return ShapeType.CIRCLE;
	}

	@Override
	public boolean intersect(final VShape shape) {
		throw new UnsupportedOperationException("not yet implemented");
	}
}
