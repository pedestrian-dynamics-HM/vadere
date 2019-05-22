package org.vadere.util.geometry.shapes;

/**
 * A {@link IPoint} represents a point in the 2D Euclidean space. Note that an {@link IPoint} might
 * be immutable (see {@link VPoint}) or mutable (see {@link MPoint}). In the mutable case operations like
 * {@link IPoint#add(IPoint)} change the 2D-coordinates of the this point.
 *
 */
public interface IPoint extends Cloneable {

	double getX();

	double getY();

	IPoint add(final IPoint point);

	IPoint addPrecise(final IPoint point);

	IPoint subtract(final IPoint point);

	IPoint multiply(final IPoint point);

	IPoint scalarMultiply(final double factor);

	IPoint rotate(final double radAngle);

	/**
	 * Computes the scalar product of this and the point.
	 *
	 * This does not effect the coordinates of this.
	 *
	 * @param point the other point
	 * @return the scalar product of this and the other point
	 */
	double scalarProduct(IPoint point);

	IPoint norm();

	IPoint norm(double len);

	 default IPoint setMagnitude(double len) {
	 	assert len >= 0;
	 	double length = distanceToOrigin();
	 	if(length <= 0) {
	 		if(len != 0.0) {
				 throw new IllegalArgumentException("a vector with zero length can not be set to a specific magnitude != 0.");
			 }
			 else {
				 return this;
			 }
		 }
		 return scalarMultiply(len / distanceToOrigin());
	 }

	 default IPoint limit(double len) {
	 	assert len >= 0;
	 	double length = distanceToOrigin();
	 	if(length > len) {
	 		return setMagnitude(len);
	    }
	    else {
	    	return this;
	    }
	 }

	IPoint normZeroSafe();

	/**
	 * Computes the Euclidean distance from this to the other.
	 *
	 * This does not effect the coordinates of this.
	 *
	 * @param other the other point
	 * @return the Euclidean distance to the other point
	 */
	double distance(IPoint other);

	/**
	 * Computes the Euclidean distance from this to the other (x,y).
	 *
	 * This does not effect the coordinates of this.
	 *
	 * @param x x-coordinate of the other point
	 * @param y y-coordinate of the other point
	 * @return the Euclidean distance to the other point
	 */
	double distance(double x, double y);

	/**
	 * Computes the squared Euclidean distance from this to the other.
	 *
	 * This does not effect the coordinates of this.
	 *
	 * @param other the other point
	 * @return the squared Euclidean distance to the other point
	 */
	double distanceSq(IPoint other);

	/**
	 * Computes the squared Euclidean distance from this to the other (x,y).
	 *
	 * This does not effect the coordinates of this.
	 *
	 * @param x x-coordinate of the other point
	 * @param y y-coordinate of the other point
	 * @return the squared Euclidean distance to the other point
	 */
	double distanceSq(double x, double y);


	/**
	 * Computes the squared Euclidean distance from this to (0,0).
	 *
	 * This does not effect the coordinates of this.
	 *
	 * @return the squared Euclidean distance to (0,0)
	 */
	double distanceToOrigin();

	/**
	 * Computes the cross product of this and the other point.
	 *
	 * This does not effect the coordinates of this.
	 *
	 * @param point the other point
	 * @return the cross product of this and the other
	 */
	default double crossProduct(IPoint point) {
		return getX() * point.getY() - point.getX() * getY();
	}

	/**
	 * Computes the dot product of this and the other point.
	 *
	 * This does not effect the coordinates of this.
	 *
	 * @param point the other point
	 * @return the dot product of this and the other
	 */
	default double dotProduct(IPoint point) {
		return (getX() * point.getX()) + (getY() * point.getY());
	}

	/**
	 * Clones the point. This will return a copy if the point
	 * is immutable, otherwise this will return this.
	 *
	 * @return a copy of the point
	 */
	IPoint clone();
}
