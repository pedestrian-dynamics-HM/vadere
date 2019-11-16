package org.vadere.util.random;

import org.vadere.util.geometry.shapes.IPoint;

/**
 * Returns a point within a bound defined by the four bound functions.
 */
public interface IPointProvider {

    double getSupportUpperBoundX();
    double getSupportLowerBoundX();
    double getSupportUpperBoundY();
    double getSupportLowerBoundY();

    IPoint nextPoint();

}
