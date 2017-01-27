package org.vadere.util.delaunay;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VTriangle;

@FunctionalInterface
public interface TriangleConstructor<P extends IPoint> {
	VTriangle create(P p1, P p2, P p3);
}
