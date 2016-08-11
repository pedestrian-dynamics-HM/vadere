package org.vadere.util.geometry;

import java.util.Comparator;

import org.vadere.util.geometry.shapes.VPoint;

/**
 * Point class representing one {@link VPoint} in 2D space with an additional
 * double-valued data store.
 * 
 * An order can be applied with the isGreaterThan method.
 * 
 */
public class DataPoint extends VPoint implements Comparable<VPoint> {

	private static final long serialVersionUID = 4631007066694627415L;

	private double data;

	public DataPoint(double x, double y, double data) {
		super(x, y);
		this.data = data;
	}

	public DataPoint(double x, double y) {
		this(x, y, 0);
	}

	public DataPoint(VPoint p, double data) {
		this(p.x, p.y, data);
	}

	public DataPoint(VPoint p1) {
		super(p1.x, p1.y);
		if (p1.getClass().equals(DataPoint.class)) {
			this.data = ((DataPoint) p1).data;
		} else {
			this.data = 0.0;
		}
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
	public void setData(double data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "(" + this.x + "," + this.y + "&" + this.data + ")";
	}

	/**
	 * Creates a comparator for the data values.
	 * 
	 * @return
	 */
	public static Comparator<DataPoint> getComparator() {
		return new Comparator<DataPoint>() {

			@Override
			public int compare(DataPoint d1, DataPoint d2) {
				if (Math.abs(d1.data - d2.data) < GeometryUtils.DOUBLE_EPS) {
					return 0;// do not compare coordinates
				} else if (d1.data < d2.data) {
					return -1;
				}
				return 1;
			}
		};
	}

	/**
	 * Creates a comparator for the data values. If the values agree, the
	 * Point.compareTo is called to compare x,y values.
	 * 
	 * @return
	 */
	public static Comparator<? super DataPoint> getPointComparator() {
		return new Comparator<DataPoint>() {
			@Override
			public int compare(DataPoint d1, DataPoint d2) {
				if (Math.abs(d1.data - d2.data) < GeometryUtils.DOUBLE_EPS) {
					// compare coordinates
					return d1.compareTo(d2);
				} else if (d1.data < d2.data) {
					return -1;
				}
				return 1;
			}
		};
	}

	/**
	 * Compares the points without using the data value of this point.
	 */
	@Override
	public int compareTo(VPoint other) {
		if (Math.abs(this.x - other.x) < GeometryUtils.DOUBLE_EPS) {
			if (Math.abs(this.y - other.y) < GeometryUtils.DOUBLE_EPS) {
				return 0;
			} else {
				if (this.y > other.y) {
					return 1;
				}
			}
		} else {
			if (this.x > other.x) {
				return 1;
			} else {
				return -1;
			}
		}
		return -1;
	}
}
