package org.vadere.simulator.models.potential.timeCostFunction.loading;

import org.vadere.state.scenario.Pedestrian;

/**
 * Inspired by Hartmann using two constant loadings. One for the pedestrians
 * with the same target and one for pedestrians with another target (c_D and
 * c_D') as discribed in hartmann-2012.
 * 
 * 
 */
class PedestrianLoadingStrategyConstant implements IPedestrianLoadingStrategy {
	private final double sameFloorLoading;
	private final double otherFloorLoading;
	private final int floorFieldTargetId;

	/**
	 * Construct a new PedestrianLoadingStrategyConstant.
	 * 
	 * @param floorFieldTargetId
	 *        the target id of the target of the generated potential field
	 * @param sameFlooLoading
	 *        c_D
	 * @param otherFloorLoading
	 *        c_D'
	 */
	PedestrianLoadingStrategyConstant(final int floorFieldTargetId,
			final double sameFlooLoading, final double otherFloorLoading) {
		this.floorFieldTargetId = floorFieldTargetId;
		this.sameFloorLoading = sameFlooLoading;
		this.otherFloorLoading = otherFloorLoading;
	}

	@Override
	public double calculateLoading(Pedestrian body) {
		return body.getNextTargetId() == floorFieldTargetId ? sameFloorLoading
				: otherFloorLoading;
	}

	@Override
	public String toString() {
		return "\n" + "c_R  = " + sameFloorLoading + "\n" + "c_R' = "
				+ otherFloorLoading;
	}
}
