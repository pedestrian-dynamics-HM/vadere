package org.vadere.util.random.offset;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Random;

/**
 * Derive a new point, uniformly selected within a given radius around
 * the given based point.
 */
public class UniformRadialPointOffset implements IPointOffsetProvider {

    private RealDistribution radiusDist;
    private RealDistribution angleDist;
    private double maxOffsetRadius;


    public UniformRadialPointOffset(final Random random, double maxOffsetRadius){
        this.maxOffsetRadius = maxOffsetRadius;
        this.radiusDist = new UniformRealDistribution(new JDKRandomGenerator(random.nextInt()), 0, 1.0);
        this.angleDist = new UniformRealDistribution(new JDKRandomGenerator(random.nextInt()), 0, 2*Math.PI);
    }

    @Override
    public IPoint applyOffset(IPoint point) {
        double radius = maxOffsetRadius*radiusDist.sample();
        double angle = angleDist.sample();
        VPoint offset = new VPoint(radius*Math.cos(angle), radius*Math.sin(angle));
        return point.add(offset);
    }

}
