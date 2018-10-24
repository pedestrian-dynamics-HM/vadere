package org.vadere.meshing.mesh.inter;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

@FunctionalInterface
public interface IPointConstructor<P extends IPoint> {
	P create(double x, double y);

	IPointConstructor<VPoint> pointConstructorVPoint = (x, y) -> new VPoint(x, y);
}
