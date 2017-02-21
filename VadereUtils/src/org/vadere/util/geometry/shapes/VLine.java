package org.vadere.util.geometry.shapes;

import java.awt.geom.Line2D;

import org.vadere.util.geometry.GeometryUtils;

@SuppressWarnings("serial")
public class VLine extends Line2D.Double {


	public VLine(VPoint p1, VPoint p2) {
		super(p1.x, p1.y, p2.x, p2.y);
	}

	public VLine(double x1, double y1, double x2, double y2) {
		super(x1, y1, x2, y2);
	}

	public double ptSegDist(VPoint point) {
		return super.ptSegDist(point.x, point.y);
	}

	public double distance(VPoint point) {
		return GeometryUtils.closestToSegment(this, point).distance(point);
	}
}
