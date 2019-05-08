package org.vadere.simulator.models.potential.timeCostFunction;

import org.vadere.simulator.models.density.IGaussianFilter;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.state.attributes.models.AttributesTimeCost;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.logging.Logger;

/**
 * TimeCostPedestrianDensityQueuing is a time cost function for the pedestrian
 * density that uses image processing filters. The function is for the purpose
 * queuing. The measurement with a loading is done by the
 * PedestrianGaussianFilter that uses the javaCV library. This has to be done
 * for every simulation step.
 * 
 * 
 */
public class TimeCostPedestrianDensityQueuing implements ITimeCostFunction {
	/** the image processing filter to measure the weighted density. */
	private final IGaussianFilter gaussianCalculator;

	/** the decorator that is used by this decorator. */
	private ITimeCostFunction timeCostFunction;

	private final double queueWidthFactor;

	// private final static double EPSILON = 0.0001;
	private final static double EPSILON = 0.000001;

	/** only for logging. */
	private static Logger logger = Logger
			.getLogger(TimeCostPedestrianDensityQueuing.class);
	private double highestCost = 0.0;
	private long runtime = 0;

	public TimeCostPedestrianDensityQueuing(
			final ITimeCostFunction timeCostFunction,
			final AttributesTimeCost attributes,
			final IGaussianFilter filter) {

		this.timeCostFunction = timeCostFunction;
		this.gaussianCalculator = filter;
		this.queueWidthFactor = attributes.getQueueWidthLoading();

		logger.info("time cost attributes:  " + attributes);
		logger.info("queueWidthFactor: " + queueWidthFactor);
		logger.info("filter: " + filter);

		// the initial filtering (convolution)
		this.gaussianCalculator.filterImage();
	}

	@Override
	public double costAt(final IPoint p) {
		long ms = System.currentTimeMillis();

		double cost = queueWidthFactor
				* gaussianCalculator.getFilteredValue(p.getX(), p.getY());

		runtime += System.currentTimeMillis() - ms;

		cost = Math.min(cost, 1.0 - EPSILON);

		if (highestCost < cost) {
			// logger.info("pedestrian density cost: " + cost);
			highestCost = cost;
		}

		return timeCostFunction.costAt(p) - cost;
	}

	@Override
	public void update() {
		// logger.info("runtime: " + runtime);
		runtime = 0;
		long ms = System.currentTimeMillis();

		// refresh the filtered image
		this.gaussianCalculator.filterImage();
		runtime += System.currentTimeMillis() - ms;
	}

	@Override
	public boolean needsUpdate() {
		return true;
	}

	@Override
	public String toString() {
		return "(pedestrian density function(x)) * " + timeCostFunction;
	}
}
