package org.vadere.util.random.offset;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Random;

public class UniformGridPointOffset implements IPointOffsetProvider {

	private RealDistribution xDist;
	private RealDistribution yDist;


	public UniformGridPointOffset(final Random random, double xMinDelta, double xMaxDelta,  double yMinDelta, double yMaxDelta){
		xDist = new UniformRealDistribution(new JDKRandomGenerator(random.nextInt()), xMaxDelta, yMaxDelta);
		yDist = new UniformRealDistribution(new JDKRandomGenerator(random.nextInt()), yMaxDelta, yMaxDelta);
	}


	@Override
	public IPoint applyOffset(IPoint point) {
		return point.addPrecise(new VPoint(xDist.sample(), yDist.sample()));
	}

}
