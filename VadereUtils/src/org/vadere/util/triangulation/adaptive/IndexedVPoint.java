package org.vadere.util.triangulation.adaptive;

import org.vadere.util.geometry.shapes.VPoint;

/**
 * Created by bzoennchen on 10.11.16.
 */
public class IndexedVPoint extends VPoint {
	private int id;

	public IndexedVPoint(final VPoint point, final int id){
		this(point.getX(), point.getY(), id);
	}

	public IndexedVPoint(final double x, final double y, final int id){
		super(x, y);
		this.id = id;
	}

	public IndexedVPoint subtract(VPoint point) {
		return new IndexedVPoint(super.subtract(point), id);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
