package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.distributions.AttributesMixedDistribution;
import org.vadere.state.scenario.distribution.DistributionFactory;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.parameter.MixedParameterDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;
import org.vadere.util.Attributes;

import java.util.ArrayList;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "mixed", parameter = AttributesMixedDistribution.class)
public class MixedDistribution extends VDistribution<AttributesMixedDistribution> {
	private Attributes mixedAttributes;
	ArrayList<Double> switchPoints;
	ArrayList<VDistribution<?>> distributions;
	private int currentInterval = 0;

	public  MixedDistribution(){
		// Do not remove this constructor. It is us used through reflection.
		super();
		this.mixedAttributes = new AttributesMixedDistribution();
	}
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

	private void setDistributions(ArrayList<MixedParameterDistribution> distributions,
	        RandomGenerator randomGenerator) throws Exception {
		this.distributions = new ArrayList<>();

		for (MixedParameterDistribution distribution : distributions) {
			VDistribution<?> dist = DistributionFactory
					.create(
							getAttributes(),
							randomGenerator
					);
			this.distributions.add(dist);
		}
	}

	@Override
	public double getNextSpawnTime(double timeCurrentEvent) {
		return getDistributionByTime(timeCurrentEvent)
				.getNextSpawnTime(timeCurrentEvent);
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
