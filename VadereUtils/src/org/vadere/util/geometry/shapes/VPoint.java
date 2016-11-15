package org.vadere.util.geometry.shapes;

import java.awt.*;
import java.awt.geom.Point2D;
import java.math.BigDecimal;

import org.vadere.util.geometry.GeometryUtils;

/**
 * Immutable point.
 * 
 * 
 */
public class VPoint {

	public static final VPoint ZERO = new VPoint(0, 0);

	public double x;
	public double y;

	public VPoint() {}

	public VPoint(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public VPoint(Point2D.Double copy) {
		this.x = copy.x;
		this.y = copy.y;
	}

	public VPoint(Point copy) {
		this.x = copy.x;
		this.y = copy.y;
	}

	public double distance(VPoint other) {
		return Point2D.distance(x, y, other.x, other.y);
	}

	public double distance(Point2D other) {
		return Point2D.distance(x, y, other.getX(), other.getY());
	}

	@Override
	public VPoint clone() {
		return new VPoint(x, y);
	}


	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof VPoint))
			return false;

		VPoint other = (VPoint) obj;

		if (this.x != other.x)
			return false;
		if (this.y != other.y)
			return false;

		return true;
	}

	public boolean equals(final VPoint point, double tolerance) {
		if (point == null) {
			return false;
		}

		if (Math.abs(this.x - point.x) > tolerance)
			return false;
		if (Math.abs(this.y - point.y) > tolerance)
			return false;

		return true;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public VPoint rotate(final double radAngle) {
		return new VPoint(x * Math.cos(radAngle) - y * Math.sin(radAngle),
				x * Math.sin(radAngle) + y * Math.cos(radAngle));
	}

	public VPoint add(final VPoint point) {
		return new VPoint(x + point.x, y + point.y);
	}

	public VPoint addPrecise(final VPoint point) {
		return VPoint.addPrecise(this, point);
	}

	public VPoint subtract(final VPoint point) {
		return new VPoint(x - point.x, y - point.y);
	}

	public VPoint multiply(final VPoint point) {
		return new VPoint(x * point.x, y * point.y);
	}

	public VPoint scalarMultiply(final double factor) {
		return new VPoint(x * factor, y * factor);
	}

	public double scalarProduct(VPoint point) {
		return x * point.x + y * point.y;
	}

	public VPoint norm() {
		double abs = distanceToOrigin();
		return new VPoint(x / abs, y / abs);
	}

	public VPoint normZeroSafe() {

		VPoint result;
		double abs = distanceToOrigin();

		if (abs < GeometryUtils.DOUBLE_EPS) {
			result = new VPoint(0, 0);
		} else {
			result = new VPoint(x / abs, y / abs);
		}

		return result;
	}

	@Override
	public String toString() {
		return "x:" + x + ", y:" + y;
	}

	public static VPoint addPrecise(final VPoint p1, final VPoint p2) {
		BigDecimal p1X = BigDecimal.valueOf(p1.getX());
		BigDecimal p2X = BigDecimal.valueOf(p2.getX());

		BigDecimal p1Y = BigDecimal.valueOf(p1.getY());
		BigDecimal p2Y = BigDecimal.valueOf(p2.getY());

		return new VPoint(p1X.add(p2X).doubleValue(), p1Y.add(p2Y).doubleValue());
	}

	public double distanceToOrigin() {
		return Math.sqrt(x * x + y * y);
	}
}
