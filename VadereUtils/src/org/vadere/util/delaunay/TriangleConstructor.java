package org.vadere.util.delaunay;

import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;

@FunctionalInterface
public interface TriangleConstructor<P extends VPoint, T extends VTriangle> {
	T create(P p1, P p2, P p3);
}
