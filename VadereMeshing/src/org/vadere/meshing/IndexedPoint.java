package org.vadere.meshing;

import org.vadere.util.geometry.shapes.VPoint;

public class IndexedPoint extends VPoint {
	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = -8480872753768346393L;

	public final int index;

	public IndexedPoint(VPoint point, int index) {
		this(point.x, point.y, index);
	}

	public IndexedPoint(double x, double y, int index) {
		super(x, y);
		this.index = index;
	}
}
