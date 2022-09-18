package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.attributes.distributions.AttributesMixedDistribution;
import org.vadere.state.scenario.distribution.DistributionFactory;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "mixed", parameter = AttributesMixedDistribution.class)
public class MixedDistribution extends VDistribution<AttributesMixedDistribution> {
	List<Double> switchPoints;
	List<VDistribution<?>> distributions;
	private int currentInterval = 0;

	public MixedDistribution(AttributesMixedDistribution parameter,RandomGenerator randomGenerator)
	        throws Exception {
		super(parameter, randomGenerator);
	}

	@Override
	protected void setValues(AttributesMixedDistribution parameter, RandomGenerator randomGenerator)
	        throws Exception {

		final boolean tooManyFewSwitchPoints = parameter.getSwitchpoints().size() != parameter.getDistributions().size() - 1;
		if (tooManyFewSwitchPoints) {
			throw new Exception("There should be exactly one switchpoint for"
			        + "every given distribution minus 1. However there are not.");
		}

		setDistributions(parameter.getDistributions(), randomGenerator);
		this.switchPoints = parameter.getSwitchpoints();

	}

	private void setDistributions(List<AttributesDistribution> distributions,
	        RandomGenerator randomGenerator) throws Exception {
		this.distributions = new ArrayList<>();

		for (AttributesDistribution distribution : distributions) {
			VDistribution<?> dist = DistributionFactory
					.create(
							distribution,
							randomGenerator
					);
			this.distributions.add(dist);
		}
	}

	@Override
	public double getNextSample(double timeCurrentEvent) {
		return getDistributionByTime(timeCurrentEvent)
				.getNextSample(timeCurrentEvent);
	}

	private VDistribution<?> getDistributionByTime(double timeCurrentEvent) {
		while (intervallAndTimeIsValidAt(timeCurrentEvent)) {
			currentInterval++;
		}
		return distributions.get(currentInterval);
	}

	private boolean intervallAndTimeIsValidAt(double timeCurrentEvent) {
		return iscCurrentIntervalInBound() && isEventInCurrentInterval(timeCurrentEvent)
				&& isTimeInBound(timeCurrentEvent);
	}

	private boolean isTimeInBound(double timeCurrentEvent) {
		return !(timeCurrentEvent > switchPoints.get(switchPoints.size() - 1));
	}

	private boolean isEventInCurrentInterval(double timeCurrentEvent) {
		return timeCurrentEvent >= switchPoints.get(currentInterval);
	}

	private boolean iscCurrentIntervalInBound() {
		return !(currentInterval > switchPoints.size() - 1);
	}

	public VDistribution<?> getCurrentDistribution() {
		return distributions.get(currentInterval);
	}

}
