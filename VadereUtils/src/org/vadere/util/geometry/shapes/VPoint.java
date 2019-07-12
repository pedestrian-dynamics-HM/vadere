package org.vadere.util.geometry.shapes;

import java.awt.*;
import java.awt.geom.Point2D;
import java.math.BigDecimal;

import org.vadere.util.geometry.GeometryUtils;

import static org.junit.Assert.assertEquals;

/**
 * Immutable point.
 * TODO: this class should be final
 * 
 */
public class VPoint implements Cloneable, IPoint {

	public static final VPoint ZERO = new VPoint(0, 0);

	public double x;
	public double y;

	public VPoint() {}

	public VPoint(final IPoint point) {
		this(point.getX(), point.getY());
	}

	public VPoint(double x, double y) {
		this.x = x;
		this.y = y;
		assert !Double.isNaN(x) && !Double.isNaN(y) && Double.isFinite(x) && Double.isFinite(y);
    }

	public VPoint(Point2D.Double copy) {
		this.x = copy.x;
		this.y = copy.y;
		assert !Double.isNaN(x) && !Double.isNaN(y) && Double.isFinite(x) && Double.isFinite(y);
	}

	public VPoint(Point2D copy) {
		this.x = copy.getX();
		this.y = copy.getY();
		assert !Double.isNaN(x) && !Double.isNaN(y) && Double.isFinite(x) && Double.isFinite(y);
	}

	public VPoint(Point copy) {
		this.x = copy.x;
		this.y = copy.y;
		assert !Double.isNaN(x) && !Double.isNaN(y) && Double.isFinite(x) && Double.isFinite(y);
	}

	@Override
	public double distance(IPoint other) {
		return distance(other.getX(), other.getY());
	}

	@Override
	public double distance(final double x, final double y) {
		return Point2D.distance(this.x, this.y, x, y);
	}

	public double distance(final Point2D other) {
		return distance(other.getX(), other.getY());
	}

	public double distanceSq(final double x, final double y) {
		return Point2D.distanceSq(this.x, this.y, x, y);
	}

	public double distanceSq(final IPoint other) {
		return Point2D.distanceSq(other.getX(), other.getY(), x, y);
	}

	@Override
	public VPoint clone() {
		return new VPoint(x, y);
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

	@Override
	public int hashCode() {
		// hashCode of java.awt.geom.Point2D
		long bits = java.lang.Double.doubleToLongBits(getX());
		bits ^= java.lang.Double.doubleToLongBits(getY()) * 31;
		return (((int) bits) ^ ((int) (bits >> 32)));
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

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	public VPoint rotate(final double radAngle) {
		return new VPoint(x * Math.cos(radAngle) - y * Math.sin(radAngle),
				x * Math.sin(radAngle) + y * Math.cos(radAngle));
	}

	@Override
	public VPoint add(final IPoint point) {
		return new VPoint(x + point.getX(), y + point.getY());
	}

	@Override
	public VPoint add(double x, double y) {
		return new VPoint(this.x + x, this.y + y);
	}

	@Override
	public VPoint addPrecise(final IPoint point) {
		return VPoint.addPrecise(this, point);
	}

	@Override
	public VPoint subtract(final IPoint point) {
		return new VPoint(x - point.getX(), y - point.getY());
	}

	@Override
	public VPoint multiply(final IPoint point) {
		return new VPoint(x * point.getX(), y * point.getY());
	}

	@Override
	public VPoint scalarMultiply(final double factor) {
		return new VPoint(x * factor, y * factor);
	}

	@Override
	public double scalarProduct(IPoint point) {
		return x * point.getX() + y * point.getY();
	}



	@Override
	public VPoint norm() {
		return norm(distanceToOrigin());
	}

	@Override
	public VPoint norm(double len) {
		return new VPoint(x / len, y / len);
	}

	@Override
	public VPoint setMagnitude(double len) {
		double length = distanceToOrigin();
		if(length <= 0) {
			if(len != 0.0) {
				throw new IllegalArgumentException("a vector with zero length can not be set to a specific magnitude != 0.");
			}
			else {
				return this;
			}
		}
		return scalarMultiply(len / distanceToOrigin());
	}

	@Override
	public VPoint limit(double len) {
		assert len >= 0;
		double length = distanceToOrigin();
		if(length > len) {
			return setMagnitude(len);
		}
		else {
			return this;
		}
	}

	@Override
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
		return "[" + x + "," + y + "]";
	}

	public static VPoint addPrecise(final IPoint p1, final IPoint p2) {
		BigDecimal p1X = BigDecimal.valueOf(p1.getX());
		BigDecimal p2X = BigDecimal.valueOf(p2.getX());

		BigDecimal p1Y = BigDecimal.valueOf(p1.getY());
		BigDecimal p2Y = BigDecimal.valueOf(p2.getY());

		return new VPoint(p1X.add(p2X).doubleValue(), p1Y.add(p2Y).doubleValue());
	}

	@Override
	public double distanceToOrigin() {
		return Math.sqrt(x * x + y * y);
	}

	public Point2D.Double asPoint2D() {
		return new Point2D.Double(x, y);
	}
}
