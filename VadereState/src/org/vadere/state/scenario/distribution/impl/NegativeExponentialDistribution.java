package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.distributions.AttributesNegativeExponentialDistribution;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "negativeExponential", parameter = AttributesNegativeExponentialDistribution.class)
public class NegativeExponentialDistribution extends VDistribution<AttributesNegativeExponentialDistribution> {
	private ExponentialDistribution distribution;

	public NegativeExponentialDistribution(AttributesNegativeExponentialDistribution parameter,
										   RandomGenerator randomGenerator) throws Exception {
		super(parameter, randomGenerator);
	}

	@Override
	protected void setValues(AttributesNegativeExponentialDistribution parameter, RandomGenerator randomGenerator)
	        throws Exception {
		this.distribution = new ExponentialDistribution(randomGenerator, parameter.getMean());
	}

	@Override
	public double getNextSample(double timeCurrentEvent) {
		return timeCurrentEvent + distribution.sample();
	}

}
