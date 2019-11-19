package org.vadere.util.geometry;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;

/**
 * A triangle with additional double data at each point.
 */
public class DataTriangle extends VTriangle {

	private static final long serialVersionUID = -8474888965422993293L;

	/**
	 * The point where the distance is measured from.
	 */
	public DataPoint measurePoint;

	/**
	 * Creates a triangle. Points must be given in ccw order.
	 * 
	 * @param p1
	 * @param p2
	 * @param p3
	 * @param mp
	 *        datapoint, holding the data of this triangle
	 */
	public DataTriangle(VPoint p1, VPoint p2, VPoint p3, DataPoint mp) {
		super(p1, p2, p3);

		this.measurePoint = mp;
	}

	/**
	 * @param p1
	 * @param p2
	 * @param p3
	 */
	public DataTriangle(DataPoint p1, DataPoint p2, DataPoint p3) {
		super(p1, p2, p3);

		recalculateMeasurePoint();
	}

	/**
	 * Recalculates the measure point based on the data stored in the DataPoint
	 * s.
	 */
	private void recalculateMeasurePoint() {
		// set measure point for gradient computations
		TreeSet<DataPoint> points = new TreeSet<DataPoint>(
				DataPoint.getPointComparator());
		points.add((DataPoint) p1);
		points.add((DataPoint) p2);
		points.add((DataPoint) p3);

		// set lowest point as measure point
		this.setMeasurePoint(points.first());
	}

	/**
	 * Sets the measure point.
	 * 
	 * @param newMeasurePoint
	 */
	public void setMeasurePoint(DataPoint newMeasurePoint) {
		this.measurePoint = newMeasurePoint;
	}

	public double evaluateAtMeasurePoint(VPoint toEval) {
		return this.measurePoint.getData() + this.measurePoint.distance(toEval);
	}

	/**
	 * Converts the given triangle in a DataTriangle. Note that if the triangle
	 * is already a DataTriangle, the data values are NOT copied but also set to
	 * 0.0.
	 * 
	 * @param triangle
	 */
	public DataTriangle(VTriangle triangle) {
		this(triangle, 0.0);
	}

	/**
	 * Converts the given triangle in a DataTriangle. Note that if the triangle
	 * is already a DataTriangle, the data values are NOT copied but also set to
	 * initialData.
	 * 
	 * @param triangle
	 *        the triangle to copy
	 * @param initialData
	 *        data to set at each vertex
	 */
	public DataTriangle(VTriangle triangle, double initialData) {
		this(new DataPoint(triangle.p1, initialData), new DataPoint(
				triangle.p2, initialData), new DataPoint(triangle.p3,
						initialData));
	}

	/**
	 * A new DataTriangle from {@link VPoint} points. The measure point is set
	 * to 0,0 with data 0.
	 * 
	 * @param p1
	 * @param p2
	 * @param p3
	 */
	public DataTriangle(VPoint p1, VPoint p2, VPoint p3) {
		this(p1, p2, p3, new DataPoint(0, 0));
	}

	/**
	 * Get data at a specified point that must equal one of the triangles
	 * vertices.
	 * 
	 * @param p
	 *        p1,p2 or p3 specified in the constructor.
	 * @throws IllegalArgumentException
	 *         if the point does not lie on any of the three vertices.
	 * @return the data value at that point
	 */
	public double getDataAt(VPoint p) {
		if (p == this.p1) {
			return ((DataPoint) p1).getData();
		}
		if (p == this.p2) {
			return ((DataPoint) p2).getData();
		}
		if (p == this.p3) {
			return ((DataPoint) p3).getData();
		}
		throw new IllegalArgumentException(p
				+ " does not lie on any of the vertices of the given triangle.");
	}

	/**
	 * Set the data at the given point.
	 * 
	 * @param p
	 *        must be one of the vertices of this triangle. if p is not any
	 *        of the vertices, nothing happens.
	 * @param data
	 *        new data at p
	 */
	public void setDataAt(VPoint p, double data) {
		if (p.equals(this.p1)) {
			((DataPoint) p1).setData(data);
		} else if (p.equals(this.p2)) {
			((DataPoint) p2).setData(data);
		} else if (p.equals(this.p3)) {
			((DataPoint) p3).setData(data);
		}

		recalculateMeasurePoint();
		// else
		// throw new IllegalArgumentException(p +
		// " does not lie on any of the vertices of the given triangle.");
	}

	public double evaluateAt(VPoint toEval) {
		// plane spanned by v1 and v2
		VPoint v1 = new VPoint(p2.x - p1.x, p2.y - p1.y);
		VPoint v2 = new VPoint(p3.x - p1.x, p3.y - p1.y);

		double[] v1d = new double[] {v1.x, v1.y,
				((DataPoint) p1).getData() - ((DataPoint) p2).getData()};
		double[] v2d = new double[] {v2.x, v2.y,
				((DataPoint) p1).getData() - ((DataPoint) p3).getData()};

		double[] cross = new double[3];
		GeometryUtils.cross(v1d, v2d, cross);
		double k = cross[2] * ((DataPoint) p1).getData();

		VPoint p1_minus_toEval = new VPoint(p1.x - toEval.x, p1.y - toEval.y);

		return 1
				/ cross[2]
				* (k - cross[0] * p1_minus_toEval.x - cross[1]
						* p1_minus_toEval.y);
	}

	public Collection<? extends DataPoint> getDataPoints() {
		List<DataPoint> dataPoints = new LinkedList<DataPoint>();
		dataPoints.add((DataPoint) p1);
		dataPoints.add((DataPoint) p2);
		dataPoints.add((DataPoint) p3);

		return dataPoints;
	}

}
