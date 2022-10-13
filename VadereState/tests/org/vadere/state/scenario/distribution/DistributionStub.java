package org.vadere.state.scenario.distribution;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.distributions.AttributesDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
public class DistributionStub extends VDistribution {

	public DistributionStub(AttributesDistribution parameter,RandomGenerator randomGenerator) throws Exception {
		super(parameter,randomGenerator);
	}

	@Override
	protected void setValues(AttributesDistribution parameter, RandomGenerator randomGenerator) throws Exception {

	}

	@Override
	public double getNextSample(double timeCurrentEvent) {
		return 0;
	}
}