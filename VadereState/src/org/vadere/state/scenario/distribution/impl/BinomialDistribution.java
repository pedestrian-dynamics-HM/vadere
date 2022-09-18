package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.distributions.AttributesBinomialDistribution;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "binomial", parameter = AttributesBinomialDistribution.class)
public class BinomialDistribution extends VDistribution<AttributesBinomialDistribution> {
	private org.apache.commons.math3.distribution.BinomialDistribution distribution;

	public BinomialDistribution(AttributesBinomialDistribution parameter,RandomGenerator randomGenerator)
			throws Exception {
		super(parameter,randomGenerator);
	}
	@Override
	protected void setValues(AttributesBinomialDistribution parameter, RandomGenerator randomGenerator) {
		this.distribution = new org.apache.commons.math3.distribution.BinomialDistribution(randomGenerator,
				parameter.getTrials(), parameter.getP());
	}
	@Override
	public double getNextSample(double timeCurrentEvent) {
		return timeCurrentEvent + distribution.sample();
	}

}
