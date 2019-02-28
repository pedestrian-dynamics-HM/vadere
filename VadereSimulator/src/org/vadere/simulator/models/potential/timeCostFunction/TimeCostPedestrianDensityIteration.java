package org.vadere.simulator.models.potential.timeCostFunction;

import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.simulator.models.potential.timeCostFunction.loading.IPedestrianLoadingStrategy;
import org.vadere.state.attributes.models.AttributesTimeCost;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.Collection;

/**
 * TimeCostPedestrianDensityIteration return cost for the pedestrian density by
 * using the gaussian formula from Seitz paper. We currently use the
 * getSpatialMapPeds for getting the neighbours (3 * size x size * 3), with size
 * = 3. The function is for the purpose of navigation around groups.
 * 
 * This implementation is not efficient, but was used to compare the values and
 * the performance of the different implementations. Use the
 * TimeCostPedestrianDensity instead.
 * 
 * 
 */
@Deprecated
public class TimeCostPedestrianDensityIteration implements ITimeCostFunction {
	/** the floor for the time cost function in general the whole scenario. */
	private final Topography floor;

	/** the for the pedestrian density function. */
	private final double scaleFactor;

	/** the varianz of the pedestrian density function. */
	private final double varianz;

	/** the next decorator (time cost function) of the decorator pattern. */
	private final ITimeCostFunction timeCostFunction;

	/** the loading of the pedestrian density (= c_p). */
	private IPedestrianLoadingStrategy loadingStrategy;

	/**
	 * a flag which decide that we use a dynamic (true) or a constant (false)
	 * pedestrian loading.
	 */
	private boolean useDynamicLoading = false;

	/** only for logging information. */
	private long runtime = 0;
	private int updateCount = 0;
	private double heighestCost = 0.0;
	private static Logger logger = Logger
			.getLogger(TimeCostPedestrianDensityIteration.class);

	TimeCostPedestrianDensityIteration(
			final ITimeCostFunction timeCostFunction, final AttributesAgent attributesPedestrian,
			final AttributesTimeCost attributes, final int targetId,
			final Topography floor) {
		this.timeCostFunction = timeCostFunction;
		this.floor = floor;
		this.varianz = attributes.getStandardDeviation()
				* attributes.getStandardDeviation();
		this.scaleFactor = attributesPedestrian.getRadius() * 2
				* attributesPedestrian.getRadius() * 2 * Math.sqrt(3) * 0.5
				/ (Math.PI * 2 * varianz);

		loadingStrategy = IPedestrianLoadingStrategy.create(floor, attributes, attributesPedestrian, targetId);
		logger.info("torso:  " + attributesPedestrian.getRadius() * 2);
		logger.info("standard derivation:  "
				+ attributes.getStandardDeviation());
		logger.info("varianz:  " + this.varianz);
		logger.info("scaleFactor (S_p):  " + this.scaleFactor);
		logger.info("use dynamic loading:  " + useDynamicLoading);
		logger.info("loading strategy:  " + loadingStrategy);
		logger.info("time cost function:  " + timeCostFunction);
	}

	@Override
	public double costAt(final IPoint p) {
		long ms = System.currentTimeMillis();
		double cost = calculatePedestrianDensity(new VPoint(p.getX(), p.getY()));

		if (cost > heighestCost) {
			heighestCost = cost;
		}
		runtime += System.currentTimeMillis() - ms;

		return timeCostFunction.costAt(p) + cost;
	}

	@Override
	public void update() {
		timeCostFunction.update();
		logger.info("runtime: " + runtime + " number of peds "
				+ floor.getElements(Pedestrian.class).size());
		runtime = 0;

		//logger.info(++updateCount + " pedestrian density cost: " + heighestCost);
		++updateCount;

	}

	@Override
	public boolean needsUpdate() {
		return true;
	}

	private double calculatePedestrianDensity(final IPoint position) {
		double densitySum = 0.0;

		double radius = 4;
		Collection<Pedestrian> pedestrianBodies = floor
				.getSpatialMap(Pedestrian.class).getObjects(new VPoint(position), radius);

		if (!pedestrianBodies.isEmpty()) {
			for (Pedestrian pedestrianBody : pedestrianBodies) {
				VPoint pedestrianPosition = pedestrianBody.getPosition();
				double distance = pedestrianPosition.distance(position);
				densitySum += Math.exp((-distance * distance * 0.5) / varianz)
						* loadingStrategy.calculateLoading(pedestrianBody);
			}
		}
		return densitySum * scaleFactor;
	}

	@Override
	public String toString() {
		return "(pedestrian density function(x)) + " + timeCostFunction;
	}
}
