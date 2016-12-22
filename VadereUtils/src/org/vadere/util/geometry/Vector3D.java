package org.vadere.util.geometry;

import java.awt.Point;

/**
 * A point in 3D.
 * 
 */
public class Vector3D extends Point implements Comparable<Vector3D> {

	private static final long serialVersionUID = 8102390144168734089L;

	public final double x;
	public final double y;
	public final double z;
	public final int index;

	public static final Vector3D ZERO = new Vector3D(0, 0, 0);

	/**
	 * Constant for comparison of double values. Everything below this is
	 * considered equal.
	 */
	public static final double DOUBLE_EPS = 1e-8;

	/**
	 * @param x
	 * @param y
	 * @param z
	 */
	public Vector3D(double x, double y, double z, int index) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.index = index;
	}

	public Vector3D(double x, double y, double z) {
		this(x, y, z, -1);
	}

	public Vector3D add(Vector3D c) {
		return new Vector3D(this.x + c.x, this.y + c.y, this.z + c.z, -1);
	}

	public Vector3D sub(Vector3D p) {
		return add(new Vector3D(-p.x, -p.y, -p.z, -1));
	}

	/**
	 * Normalizes the given point (now considered a vector) to the given length.
	 * If it is the zero vector, it is returned unchanged.
	 * 
	 * @param length
	 * @return
	 */
	public Vector3D normalize(double length) {
		if (this.equals(ZERO)) {
			return ZERO;
		}
		return new Vector3D(x / distTo(ZERO) * length, y / distTo(ZERO)
				* length, z / distTo(ZERO) * length);
	}

	/**
	 * Euclidian distance to a given point.
	 * 
	 * @param p
	 * @return
	 */
	public double distTo(Vector3D p) {
		double x = this.x - p.x;
		double y = this.y - p.y;
		double z = this.z - p.z;
		return Math.sqrt(x * x + y * y + z * z);
	}

	/**
	 * Interpolates a point between this to a given target with the given
	 * factor.<br>
	 * Formula: result = this + factor*(target-this)
	 * 
	 * @param target
	 * @param factor
	 * @return
	 */
	public Vector3D interpolate(Vector3D target, double factor) {
		Vector3D result = new Vector3D(x + factor * (target.x - x), y + factor
				* (target.y - y), z + factor * (target.z - z));
		return result;
	}

	/**
	 * Checks whether the given point is greater than the current point with
	 * respect to:<br>
	 * 1. x-coordinate -> 2. y-coordinate -> 3. z-coordinate
	 * 
	 * @param p
	 *        point to compare with
	 * @return true if the current point is greater than p, false otherwise.
	 */
	public boolean isGreaterThan(Vector3D p) {
		if (this.x > p.x + DOUBLE_EPS) {
			return true;
		}
		if (Math.abs(this.x - p.x) < DOUBLE_EPS && this.y > p.y + DOUBLE_EPS) {
			return true;
		}
		if (Math.abs(this.x - p.x) < DOUBLE_EPS
				&& Math.abs(this.y - p.y) < DOUBLE_EPS
				&& this.z > p.z + DOUBLE_EPS) {
			return true;
		}
		return false;
	}

	/**
	 * Checks wether the given point is greater than the current point with
	 * respect to:<br>
	 * 1. x-coordinate -> 2. y-coordinate -> -> 3. z-coordinate
	 * 
	 * @param p
	 *        point to compare with
	 * @return 1 if the current point is greater than p, -1 if smaller, 0
	 *         otherwise.
	 */
	@Override
	public int compareTo(Vector3D p) {
		if (isGreaterThan(p)) {
			return 1;
		}
		if (Math.abs(p.x - this.x) < DOUBLE_EPS
				&& Math.abs(p.y - this.y) < DOUBLE_EPS
				&& Math.abs(p.z - this.z) < DOUBLE_EPS) {
			return 0;
		}
		return -1;
	}

	/**
	 * Uses compareTo to implement the object.equals method.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		Vector3D objP = (Vector3D) obj;

		if (this.compareTo(objP) == 0) {
			return true;
		}
		return false;
	};

	@Override
	public int hashCode() {
		return (int) (this.x * 93563 + this.y * 17 + this.z);
	}

	@Override
	public String toString() {
		return "(" + this.x + "," + this.y + "," + this.z + ")";
	}

	public Vector3D multiply(double factor) {
		return new Vector3D(this.x * factor, this.y * factor, this.z * factor);
	}

	/**
	 * Computes the cross product of two vectors and store it in the cross
	 * vector.
	 * 
	 * @param v1
	 * @param v2
	 * @param cross
	 */
	public Vector3D cross(Vector3D p2) {
		return new Vector3D(this.y * p2.z - this.z * p2.y, this.z * p2.x
				- this.x * p2.z, this.x * p2.y - this.y * p2.x);
	}
}
