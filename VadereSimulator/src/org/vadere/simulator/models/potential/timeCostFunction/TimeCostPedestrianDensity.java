package org.vadere.simulator.models.potential.timeCostFunction;

import org.vadere.simulator.models.density.IGaussianFilter;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.logging.Logger;

/**
 * TimeCostPedestrianDensity is a time cost function for the pedestrian density
 * that uses image processing filters. The function is for the purpose of
 * navigation around groups. The measurement with a loading is done by the
 * PedestrianGaussianFilter that uses the javaCV library. This has to be done
 * for every simulation step.
 * 
 * 
 */
public class TimeCostPedestrianDensity implements ITimeCostFunction {
	/** the radius for the measurement . */
	private double heighestDensity = 0.0;

	/** the image processing filter. */
	private final IGaussianFilter gaussianCalculator;

	/** the decorator that will be uesed by this decorator. */
	private ITimeCostFunction timeCostFunction;

	/** only for logging informations. */
	private static Logger logger = Logger
			.getLogger(TimeCostPedestrianDensity.class);
	private int updateCount = 0;
	private long runtime = 0;
	private Topography topography;

	public TimeCostPedestrianDensity(final ITimeCostFunction timeCostFunction, final IGaussianFilter filter) {
		this.timeCostFunction = timeCostFunction;
		this.gaussianCalculator = filter;

		logger.info("timeCostFunction:  " + timeCostFunction);
		logger.info("gaussianCalculator:  " + filter);

		// the initial filtering (convolution), TODO: we have to destroy the filter if it is no longer needed!
		this.gaussianCalculator.filterImage();
	}

	@Override
	public double costAt(final IPoint p) {
		long ms = System.currentTimeMillis();
		double cost = gaussianCalculator.getFilteredValue(p.getX(), p.getY());

		if (heighestDensity < cost) {
			heighestDensity = cost;
		}

		runtime += System.currentTimeMillis() - ms;
		return timeCostFunction.costAt(p) + cost;
		// return timeCostFunction.costAt(p) + cost < 0.7 ? 0.0 : cost;
	}

	@Override
	public void update() {
		runtime = 0;
		long ms = System.currentTimeMillis();

		// refersh the filtered image
		this.gaussianCalculator.filterImage();
		runtime += System.currentTimeMillis() - ms;
		//logger.info(++updateCount + " pedestrian density cost: "+ heighestDensity + ", runtime: " + runtime + "[msec]");
		updateCount++;
	}

	@Override
	public boolean needsUpdate() {
		return true;
	}

	@Override
	public String toString() {
		return "(pedestrian density function(x)) + " + timeCostFunction;
	}
}
