package org.vadere.util.geometry.shapes;

import org.vadere.util.geometry.GeometryUtils;

public class Vector2D extends VPoint {

	/**
	 * generated serial version UID
	 * 
	 */
	private static final long serialVersionUID = -9086115463015728807L;
	public static final VPoint ZERO = new VPoint(0, 0);

	public Vector2D() {}

	public Vector2D(double x, double y) {
		super(x, y);
	}

	public Vector2D(IPoint p) {
		super(p.getX(), p.getY());
	}

	@Override
	public Vector2D clone() {
		return new Vector2D(x, y);
	}

	public Vector2D normalize(double length) {
		double rx, ry;
		double vl = distance(ZERO);
		if (Math.abs(x) < GeometryUtils.DOUBLE_EPS) {
			rx = 0;
		} else {
			rx = x / vl * length;
		}
		if (Math.abs(y) < GeometryUtils.DOUBLE_EPS) {
			ry = 0;
		} else {
			ry = y / vl * length;
		}
		return new Vector2D(rx, ry);
	}

	public Vector2D multiply(double factor) {
		return new Vector2D(this.x * factor, this.y * factor);
	}

	public double getLength() {
		return Math.sqrt(x * x + y * y);
	}

	/**
	 * Computes the angle3D between the x-axis through the given Point (0,0) and this.
	 * Result is in interval (0,2*PI) according to standard math usage.
	 *
	 * @return the angle3D between the x-axis through the given Point (0,0) and this
	 */
	public double angleToZero() {
		double atan2 = Math.atan2(this.y, this.x);

		if (atan2 < 0.0) {
			atan2 = Math.PI * 2 + atan2;
		}

		return atan2;
	}

	/**
	 * Computes the angle3D between the x-axis through the given Point center and this.
	 * Result is in interval (0,2*PI) according to standard math usage.
	 *
	 * @param center the given center
	 * @return the angle3D between the x-axis through the given Point center and this
	 */
	public double angleTo(VPoint center) {
		return GeometryUtils.angleTo(this, center);
	}

	public Vector2D add(VPoint p) {
		return new Vector2D(this.x + p.x, this.y + p.y);
	}

	public Vector2D sub(VPoint p) {
		return new Vector2D(this.x - p.x, this.y - p.y);
	}

	public Vector2D rotate(final double radAngle) {
		return new Vector2D(x * Math.cos(radAngle) - y * Math.sin(radAngle),
				x * Math.sin(radAngle) + y * Math.cos(radAngle));
	}

}
