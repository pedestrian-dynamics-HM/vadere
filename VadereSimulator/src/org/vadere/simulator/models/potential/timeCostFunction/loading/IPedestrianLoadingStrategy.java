package org.vadere.simulator.models.potential.timeCostFunction.loading;

import java.util.function.Predicate;

import org.vadere.state.attributes.models.AttributesTimeCost;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

/**
 * IPedestrianLoadingStrategy is the interface for the strategy-pattern. A
 * pedestrian loading strategy calculates a loading based on the information of
 * the body of the pedestrian (for example the position the speed and so on).
 * 
 * 
 */
public interface IPedestrianLoadingStrategy<T extends Pedestrian> {
	/**
	 * Returns the calculated loading for the body of a pedestrian.
	 * 
	 * @param body
	 *        the body of the pedestrian
	 * @return the calculated loading for the body of a pedestrian
	 */
	double calculateLoading(final T body);

	/**
	 * Factory method.
	 * 
	 * @param topography
	 * @param timeCostAttributes
	 * @param targetId
	 * @return
	 */
	static IPedestrianLoadingStrategy create(final Topography topography,
			final AttributesTimeCost timeCostAttributes, final AttributesAgent attributesPedestrian,
			final int targetId) {
		IPedestrianLoadingStrategy loadingStrategy = null;
		switch (timeCostAttributes.getLoadingType()) {
			case DYNAMIC:
				throw new UnsupportedOperationException(
						"the dynamic generation (potential differences) of the floor field is not supported.");
				/*
				 * loadingStrategy = new PedestrianLoadingStrategyPotentialDifference(
				 * topography,
				 * targetId,
				 * timeCostAttributes.getPedestrianDynamicWeight(),
				 * attributesPedestrian.getSpeedDistributionMean(),
				 * new PotentialFieldSingleTargetGrid(topography, attributesPedestrian, new
				 * AttributesFloorField(), targetId)
				 * );
				 * 
				 * break;
				 */
			case CONSTANT_RESPECT_TARGETS:
				loadingStrategy = new PedestrianLoadingStrategyConstant(
						targetId,
						timeCostAttributes.getPedestrianSameTargetDensityWeight(),
						timeCostAttributes.getPedestrianOtherTargetDensityWeight());
				break;
			case CONSTANT:
				loadingStrategy = new PedestrianLoadingStrategyUnit(timeCostAttributes.getPedestrianWeight());
				break;
			case QUEUEGAME:
				loadingStrategy =
						new PedestrianLoadingStrategyUnitQueueingGame(timeCostAttributes.getPedestrianWeight());
				break;
			default:
				throw new IllegalArgumentException("laoding type is not supported.");
		}

		return loadingStrategy;
	}

	static <E extends Pedestrian> IPedestrianLoadingStrategy create(
			final IPedestrianLoadingStrategy loadingStrategy, final Predicate<E> predicate) {
		return new PedestrianLoadingStrategyConditioned(loadingStrategy, predicate);
	}

	static IPedestrianLoadingStrategy create() {
		return create(1.0);
	}

	static IPedestrianLoadingStrategy create(final double loading) {
		return new PedestrianLoadingStrategyUnit(loading);
	}
}
