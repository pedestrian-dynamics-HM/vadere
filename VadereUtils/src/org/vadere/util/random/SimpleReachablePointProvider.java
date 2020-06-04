package org.vadere.util.random;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.math.IDistanceFunction;

import java.awt.geom.Rectangle2D;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SimpleReachablePointProvider implements IReachablePointProvider {


	private IPointProvider iPointProvider;
	private IDistanceFunction distanceFunction;

	public static SimpleReachablePointProvider constant(double resolution, Rectangle2D.Double bound, IDistanceFunction distanceFunction){
		IPointProvider constant = new ConstantIntegerPointProvider(
				(int)Math.floor(bound.width / resolution),
				(int)Math.floor(bound.height / resolution));

		return new SimpleReachablePointProvider(constant, distanceFunction);
	}

	public static SimpleReachablePointProvider uniform(final Random random, Rectangle2D.Double bound, IDistanceFunction distanceFunction){
		IPointProvider uniform = new SimpleUniformRealPointProvider(random,
				bound.x, bound.x + bound.width,
				bound.y, bound.y + bound.height);
		return new SimpleReachablePointProvider(uniform, distanceFunction);
	}


	private SimpleReachablePointProvider(IPointProvider provider, IDistanceFunction dist){
		iPointProvider = provider;
		distanceFunction = dist;
	}

	private IPoint get(Predicate<Double> obstacleDistPredicate){
		boolean legalState;
		IPoint p;
		do {
			p = iPointProvider.nextPoint();
			double distance = -distanceFunction.apply(p);
			legalState = distance > 0 && obstacleDistPredicate.test(distance);
		} while (!legalState);
		return p;
	}

	@Override
	public Stream<IPoint> stream(Predicate<Double> obstacleDistPredicate) {
		return Stream.generate(() -> get(obstacleDistPredicate));
	}

	@Override
	public double getSupportUpperBoundX() {
		return iPointProvider.getSupportUpperBoundX();
	}

	@Override
	public double getSupportLowerBoundX() {
		return iPointProvider.getSupportLowerBoundX();
	}

	@Override
	public double getSupportUpperBoundY() {
		return iPointProvider.getSupportUpperBoundY();
	}

	@Override
	public double getSupportLowerBoundY() {
		return iPointProvider.getSupportLowerBoundY();
	}

	@Override
	public IPoint nextPoint() {
		return get(aDouble -> true);
	}
}
