package org.vadere.util.random;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Random;

public class SimpleUniformRealPointProvider implements IPointProvider {

	private RealDistribution xDist;
	private RealDistribution yDist;


	public SimpleUniformRealPointProvider(final Random random, double xUpperBound,  double yUpperBound){
		xDist = new UniformRealDistribution(new JDKRandomGenerator(random.nextInt()), 0.0, xUpperBound);
		yDist = new UniformRealDistribution(new JDKRandomGenerator(random.nextInt()), 0.0, yUpperBound);
	}


	public SimpleUniformRealPointProvider(final Random random, double xLowerBound, double xUpperBound,  double yLowerBound, double yUpperBound){
		xDist = new UniformRealDistribution(new JDKRandomGenerator(random.nextInt()), xLowerBound, xUpperBound);
		yDist = new UniformRealDistribution(new JDKRandomGenerator(random.nextInt()), yLowerBound, yUpperBound);
	}


	@Override
	public double getSupportUpperBoundX() {
		return xDist.getSupportUpperBound();
	}

	@Override
	public double getSupportLowerBoundX() {
		return xDist.getSupportLowerBound();
	}

	@Override
	public double getSupportUpperBoundY() {
		return yDist.getSupportUpperBound();
	}

	@Override
	public double getSupportLowerBoundY() {
		return yDist.getSupportLowerBound();
	}

	@Override
	public IPoint nextPoint() {
		return new VPoint(xDist.sample(), yDist.sample());
	}
}
