package org.vadere.meshing.mesh.inter;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * The {@link IPointConstructor} detach the construction of new points from their constructor
 * such that generic classes can use the point constructor instead.
 *
 * @param <P> the type of the points
 */
@FunctionalInterface
public interface IPointConstructor<P extends IPoint> {

	/**
	 * Creates a new point.
	 *
	 * @param x x-coordinate of the point
	 * @param y y-coordinate of the point
	 * @return a point of type {@link P} at (x,y)
	 */
	P create(double x, double y);

	/**
	 * A point constructor vor {@link VPoint}
	 */
	IPointConstructor<IPoint> pointConstructorVPoint = (x, y) -> new VPoint(x, y);
}
