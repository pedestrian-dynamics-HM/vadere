package org.vadere.util.random;

import org.vadere.util.geometry.shapes.IPoint;

import java.util.stream.Stream;

public interface RandomIPointProvider {

    double getSupportUpperBoundX();
    double getSupportLowerBoundX();
    double getSupportUpperBoundY();
    double getSupportLowerBoundY();

    IPoint nextPoint();

}
