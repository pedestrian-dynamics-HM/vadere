package org.vadere.simulator.models.potential.timeCostFunction.loading;

import java.util.function.Predicate;

import org.vadere.state.scenario.Pedestrian;

public class PedestrianLoadingStrategyConditioned<T extends Pedestrian> implements IPedestrianLoadingStrategy<T> {

	private IPedestrianLoadingStrategy loadingStrategy;
	private Predicate<T> predicate;

	PedestrianLoadingStrategyConditioned(final IPedestrianLoadingStrategy loadingStrategy,
			final Predicate<T> predicate) {
		this.loadingStrategy = loadingStrategy;
		this.predicate = predicate;
	}

	@Override
	public double calculateLoading(final T body) {
		if (predicate.test(body)) {
			return loadingStrategy.calculateLoading(body);
		} else {
			return 0;
		}
	}
}
