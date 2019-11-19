package org.vadere.util.random;

import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Random;

public class SimpleUniformIntegerPointProvider implements IPointProvider {

    private IntegerDistribution xDist;
    private IntegerDistribution yDist;


    public SimpleUniformIntegerPointProvider(final Random random, int xUpperBound, int yUpperBound){
        xDist = new UniformIntegerDistribution(new JDKRandomGenerator(random.nextInt()), 0, xUpperBound);
        yDist = new UniformIntegerDistribution(new JDKRandomGenerator(random.nextInt()), 0, yUpperBound);
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
