package org.vadere.util.delaunay;

import org.vadere.util.geometry.shapes.VPoint;

@FunctionalInterface
public interface PointConstructor<P extends VPoint> {
	P create(double x, double y);
}
