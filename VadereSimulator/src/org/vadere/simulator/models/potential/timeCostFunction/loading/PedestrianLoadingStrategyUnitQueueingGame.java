package org.vadere.simulator.models.potential.timeCostFunction.loading;

import org.vadere.simulator.models.queuing.QueueingGamePedestrian;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.types.PedestrianAttitudeType;

public class PedestrianLoadingStrategyUnitQueueingGame<T extends Pedestrian>
		extends PedestrianLoadingStrategyConditioned<T> {


	/**
	 *
	 * @param loading
	 */
	PedestrianLoadingStrategyUnitQueueingGame(final double loading) {
		super(new PedestrianLoadingStrategyUnit(loading), ped -> ped.getModelPedestrian(QueueingGamePedestrian.class)
				.getAttituteType() != PedestrianAttitudeType.COMPETITIVE);
	}

	@Override
	public String toString() {
		return super.toString() + " (conditioned)";
	}
}
