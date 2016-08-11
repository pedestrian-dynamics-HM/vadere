package org.vadere.simulator.models.potential.timeCostFunction.loading;

import org.vadere.simulator.models.potential.fields.PotentialFieldSingleTargetGrid;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;

/**
 * Factory that creates all the different laoding strategies.
 * 
 * 
 */
@Deprecated
public class LoadingFactory {
	/**
	 * Creates and returns the laoding constant laoding strategy (that is
	 * discribed in hartmanns paper).
	 * 
	 * @param floorFieldTargetId
	 *        the target id that is the target of the generated potential
	 *        field that uses this loading strategy
	 * @param sameFlooLoading
	 *        the loading for pedestrians of that have the same target = the
	 *        target of the generated potential field (c_D in hartmann-2012)
	 * @param otherFloorLoading
	 *        the loading for pedestrians of that have on other target (c_D'
	 *        in hartmann-2012)
	 * @return the laoding constant laoding strategy
	 */
	public static IPedestrianLoadingStrategy createConstantPedestrianLoading(
			final int floorFieldTargetId, final double sameFlooLoading,
			final double otherFloorLoading) {
		return new PedestrianLoadingStrategyConstant(floorFieldTargetId,
				sameFlooLoading, otherFloorLoading);
	}

	/**
	 * Creates and returns the laoding dynamic laoding strategy.
	 * 
	 * @param floor
	 *        the floor that contains all the pedestrian that count.
	 * @param floorFieldTargetId
	 *        the target id that is the target of the generated potential
	 *        field that uses this loading strategy
	 * @param loading
	 *        the loading that will be multiplied to the dynamic loading
	 *        (c_p in the bachelor thesis of Benedikt Zoennchen)
	 * @param meanSpeed
	 *        the mean speed of all pedestrians
	 * @return the laoding dynamic laoding strategy
	 */
	public static IPedestrianLoadingStrategy createDynamicPedestrianLoading(
			final Topography floor, final int floorFieldTargetId,
			final double loading, final double meanSpeed) {
		throw new UnsupportedOperationException(
				"the dynamic generation (potential differences) of the floor field is not supported.");
		/*
		 * return new PedestrianLoadingStrategyPotentialDifference(floor,
		 * floorFieldTargetId, loading, meanSpeed,
		 * new PotentialFieldSingleTargetGrid(floor, new AttributesPedestrian(-1), new
		 * AttributesFloorField(), floorFieldTargetId));
		 */
	}

	/**
	 * Creates and returns the laoding constant laoding strategy.
	 * 
	 * @param loading
	 *        (c in hartmann-2012)
	 * @return the laoding constant laoding strategy
	 */
	public static IPedestrianLoadingStrategy createUnitQueueGamePedestrianLoading(
			final double loading) {
		return new PedestrianLoadingStrategyUnitQueueingGame(loading);
	}

	/**
	 * Creates and returns the laoding constant laoding strategy.
	 * 
	 * @param loading
	 *        (c in hartmann-2012)
	 * @return the laoding constant laoding strategy
	 */
	public static IPedestrianLoadingStrategy createUnitPedestrianLoading(
			final double loading) {
		return new PedestrianLoadingStrategyUnit(loading);
	}
}
