package org.vadere.util.random;

import org.vadere.util.geometry.shapes.IPoint;

import java.util.function.Predicate;
import java.util.stream.Stream;

public interface IReachablePointProvider {

    void setRandomIPointProvider(RandomIPointProvider provider);
    RandomIPointProvider getRandomIPointProvider();

    void setIPointOffsetProvider(IPointOffsetProvider provider);
    IPointOffsetProvider getIPointOffsetProvider();


    Stream<IPoint> randomStream(Predicate<Double> obstacleDistPredicate);

    Stream<IPoint> stream(Predicate<Double> obstacleDistPredicate);

}
