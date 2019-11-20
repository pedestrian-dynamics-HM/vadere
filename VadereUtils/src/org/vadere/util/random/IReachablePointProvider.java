package org.vadere.util.random;

import org.vadere.util.geometry.shapes.IPoint;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Provides a stream of points legally reachable by a pedestrian in a topography.
 * The order of points in {@link #stream(Predicate)} is defined by the implementation.
 *
 * The #obstacleDistPredicate will filter each legally reachable point based on the
 * distance to its nearest obstacle. A standard usage would be a stream of reachable
 * points where all points have a distance of at least x meters away from an obstacle.
 *
 */
public interface IReachablePointProvider extends IPointProvider{

    Stream<IPoint> stream(Predicate<Double> obstacleDistPredicate);

    default Stream<IPoint> stream(){
        return stream(aDouble -> true);
    }

}
