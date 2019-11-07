package org.vadere.util.random;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Random;

public class UniformPointOffsetDouble implements IPointOffsetProvider {

    private RealDistribution radiusDist;
    private RealDistribution angleDist;
    private double maxOffsetRadius;


    public UniformPointOffsetDouble(final Random random, double maxOffsetRadius){
        this.maxOffsetRadius = maxOffsetRadius;
        this.radiusDist = new UniformRealDistribution(new JDKRandomGenerator(random.nextInt()), 0, 1.0);
        this.angleDist = new UniformRealDistribution(new JDKRandomGenerator(random.nextInt()), 0, 2*Math.PI);
    }

    @Override
    public IPoint applyOffset(IPoint point) {
        return applyOffset(point, maxOffsetRadius);
    }

    @Override
    public IPoint applyOffset(IPoint point, double maxOffset) {
        double radius = maxOffset*radiusDist.sample();
        double angle = angleDist.sample();
        VPoint offset = new VPoint(radius*Math.cos(angle), radius*Math.sin(angle));
        return point.add(offset);
    }
}
