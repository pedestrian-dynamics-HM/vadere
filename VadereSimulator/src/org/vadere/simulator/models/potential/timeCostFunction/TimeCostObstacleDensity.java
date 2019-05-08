package org.vadere.simulator.models.potential.timeCostFunction;

import org.vadere.simulator.models.density.IGaussianFilter;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.logging.Logger;

/**
 * TimeCostObstacleDensity is a time cost function for the obstacle density that
 * uses image processing filters. The measurement with a loading is done by the
 * ObstacleGaussianFilter that uses the javaCV library. This has to be done only
 * once, because the obstacles don't move.
 * 
 * 
 */
public class TimeCostObstacleDensity implements ITimeCostFunction {
	/**
	 * the image filter for the measurement of the density by calculating the
	 * integral over the obstacle area by using the convolution.
	 */
	private final IGaussianFilter obstacleImageFilter;

	/** the loading of the obstacle density (= c_o). */
	private final double loading;

	/** the next decorator (time cost function) of the decorator pattern. */
	private final ITimeCostFunction timeCostFunction;

	/** only for logging. */
	private static Logger logger = Logger
			.getLogger(TimeCostObstacleDensity.class);
	private double highest = -1.0;


	/**
	 * Construct a new TimeCostObstacleDensity-Decorator.
	 *
	 * @param timeCostFunction
	 * @param obstacleLoading
	 * @param filter
	 */
	public TimeCostObstacleDensity(final ITimeCostFunction timeCostFunction, final double obstacleLoading,
			final IGaussianFilter filter) {
		this.timeCostFunction = timeCostFunction;
		this.loading = obstacleLoading;
		this.obstacleImageFilter = filter;

		// print information
		logger.info("filter:  " + filter);
		logger.info("obstacleWeight:  " + loading);
		logger.info("time cost function:  " + timeCostFunction);
		// logger.info("floor: " + floor.getId());

		// measurethe weighted density once!
		this.obstacleImageFilter.filterImage();
	}

	@Override
	public double costAt(final IPoint p) {
		double obstacleDensity = 0.0;
		obstacleDensity = obstacleImageFilter.getFilteredValue(p.getX(), p.getY());
		if (obstacleDensity > highest) {
			highest = obstacleDensity;
			// logger.info("obstacle density: " + obstacleDensity);
		}

		return timeCostFunction.costAt(p) + obstacleDensity * loading;
	}

	@Override
	public void update() {
		timeCostFunction.update();
	}

	@Override
	public boolean needsUpdate() {
		return timeCostFunction.needsUpdate();
	}

	@Override
	public String toString() {
		return "(obstacle density function(x)) + " + timeCostFunction;
	}
}
