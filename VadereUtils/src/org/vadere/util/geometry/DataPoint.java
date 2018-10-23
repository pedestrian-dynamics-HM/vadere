package org.vadere.util.geometry;

import java.util.Comparator;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * Point class representing one {@link VPoint} in 2D space with an additional
 * double-valued data store.
 * 
 * An order can be applied with the isGreaterThan method.
 * 
 */
public class DataPoint extends VPoint implements Comparable<VPoint> {

	private static  Comparator<DataPoint> comparator = (d1, d2) -> {
		if (Math.abs(d1.data - d2.data) < GeometryUtils.DOUBLE_EPS) {
			return 0;// do not compare coordinates
		} else if (d1.data < d2.data) {
			return -1;
		}
		return 1;
	};

	private static  Comparator<DataPoint> pointComparator = (d1, d2) -> {
		if (Math.abs(d1.data - d2.data) < GeometryUtils.DOUBLE_EPS) {
			// compare coordinates
			return d1.compareTo(d2);
		} else if (d1.data < d2.data) {
			return -1;
		}
		return 1;
	};

	private double data;

	public DataPoint(final double x, final double y, final double data) {
		super(x, y);
		this.data = data;
	}

	public DataPoint(final double x, final double y) {
		this(x, y, 0);
	}

	public DataPoint(final VPoint p, final double data) {
		this(p.getX(), p.getY(), data);
	}

	/**
	 * @return the data
	 */
	public double getData() {
		return data;
	}

	/**
	 * @param data
	 *        the data to set
	 */
	public void setData(final double data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "(" + this.getX() + "," + this.getY() + "&" + this.data + ")";
	}

	/**
	 * Creates a comparator for the data values.
	 * 
	 * @return
	 */
	public static Comparator<? super DataPoint> getComparator() {
		return comparator;
	}

	/**
	 * Creates a comparator for the data values. If the values agree, the
	 * Point.compareTo is called to compare x,y values.
	 * 
	 * @return
	 */
	public static Comparator<? super DataPoint> getPointComparator() {
		return pointComparator;
	}

	/**
	 * Compares the points without using the data value of this point.
	 */
	@Override
	public int compareTo(final VPoint other) {
		if (Math.abs(getX() - other.getX()) < GeometryUtils.DOUBLE_EPS) {
			if (Math.abs(getY() - other.getY()) < GeometryUtils.DOUBLE_EPS) {
				return 0;
			} else {
				if (getY() > other.getY()) {
					return 1;
				}
			}
		} else {
			if (this.getX() > other.getX()) {
				return 1;
			} else {
				return -1;
			}
		}
		return -1;
	}
}
