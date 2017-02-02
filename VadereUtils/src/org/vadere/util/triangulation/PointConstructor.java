package org.vadere.util.triangulation;

import org.vadere.util.geometry.shapes.IPoint;

@FunctionalInterface
public interface PointConstructor<P extends IPoint> {
	P create(double x, double y);
}
