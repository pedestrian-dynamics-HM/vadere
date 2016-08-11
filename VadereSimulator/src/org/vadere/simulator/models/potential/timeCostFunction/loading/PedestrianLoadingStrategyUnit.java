package org.vadere.simulator.models.potential.timeCostFunction.loading;

import org.vadere.state.scenario.Pedestrian;

/**
 * The PedestrianLoadingStrategyUnit simply returns a constant loading for each
 * body, so each body will have the same weight. This is equals to c in
 * hartmann-2012.
 * 
 * 
 */
class PedestrianLoadingStrategyUnit implements IPedestrianLoadingStrategy {
	/** the constant loading. */
	private final double loading;

	/**
	 * Construct a new PedestrianLoadingStrategyUnit object.
	 * 
	 * @param loading
	 *        the constant loading
	 */
	PedestrianLoadingStrategyUnit(final double loading) {
		this.loading = loading;
	}

	@Override
	public double calculateLoading(Pedestrian body) {
		return loading;
	}

	@Override
	public String toString() {
		return "constant\n c = " + loading;
	}
}
