package org.vadere.meshing.mesh.inter.mesh;

import org.vadere.util.geometry.shapes.IPoint;

/**
 * A vertex represents a point in space.
 * It may be connected to other vertices by half-edges {@link IHalfEdge},
 * which together can form a face {@link IFace}.
 * A single vertex can serve as the endpoint of multiple half-edges.
 *
 * @author Benedikt Zoennchen
 *
 */
public interface IVertex extends IPoint {

	IPoint getPoint();

	@Override
	default double getX() {
		return getPoint().getX();
	}

	@Override
	default double getY() {
		return getPoint().getY();
	}

	@Override
	default IPoint add(final IPoint point) {
		return getPoint().add(point);
	}

	@Override
	default IPoint addPrecise(IPoint point) {
		return getPoint().addPrecise(point);
	}

	@Override
	default IPoint subtract(IPoint point) {
		return getPoint().subtract(point);
	}

	@Override
	default IPoint multiply(IPoint point) {
		return getPoint().multiply(point);
	}

	@Override
	default IPoint scalarMultiply(double factor) {
		return getPoint().scalarMultiply(factor);
	}

	@Override
	default IPoint rotate(double radAngle) {
		return getPoint().rotate(radAngle);
	}

	@Override
	default double scalarProduct(IPoint point) {
		return getPoint().scalarProduct(point);
	}

	@Override
	default IPoint norm() {
		return getPoint().norm();
	}

	@Override
	default IPoint normZeroSafe() {
		return getPoint().normZeroSafe();
	}

	@Override
	default double distance(final IPoint other) {
		return getPoint().distance(other);
	}

	@Override
	default double distance(double x, double y) {
		return getPoint().distance(x, y);
	}

	@Override
	default double distanceToOrigin() {
		return getPoint().distanceToOrigin();
	}
}
