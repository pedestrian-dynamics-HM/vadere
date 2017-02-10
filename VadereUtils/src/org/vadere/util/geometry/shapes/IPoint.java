package org.vadere.util.geometry.shapes;

public interface IPoint {

	double getX();

	double getY();

	IPoint add(final IPoint point);

	IPoint addPrecise(final IPoint point);

	IPoint subtract(final IPoint point);

	IPoint multiply(final IPoint point);

	IPoint scalarMultiply(final double factor);

	IPoint rotate(final double radAngle);

	double scalarProduct(IPoint point);

	IPoint norm();

	IPoint normZeroSafe();

	double distance(IPoint other);

	double distanceToOrigin();

	default double crossProduct(IPoint point) {
		return getX() * point.getY() - point.getX() * getY();
	}
}
