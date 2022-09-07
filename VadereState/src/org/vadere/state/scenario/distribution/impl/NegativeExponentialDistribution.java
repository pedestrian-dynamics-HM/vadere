package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.distributions.AttributesNegativeExponentialDistribution;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;
import org.vadere.util.Attributes;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "negativeExponential", parameter = AttributesNegativeExponentialDistribution.class)
public class NegativeExponentialDistribution extends VDistribution<AttributesNegativeExponentialDistribution> {
	private Attributes exponAttributes;
	private ExponentialDistribution distribution;

	public NegativeExponentialDistribution(){
		// Do not remove this constructor. It is us used through reflection.
		super();
		this.exponAttributes = new AttributesNegativeExponentialDistribution();
	}
	public NegativeExponentialDistribution(AttributesNegativeExponentialDistribution parameter,
										   RandomGenerator randomGenerator) throws Exception {
		super(parameter, randomGenerator);
	}

	@Override
	protected void setValues(AttributesNegativeExponentialDistribution parameter, RandomGenerator randomGenerator)
	        throws Exception {
		this.distribution = new ExponentialDistribution(randomGenerator, parameter.getMean());
		this.exponAttributes = parameter;
	}

	@Override
	public double getNextSpawnTime(double timeCurrentEvent) {
		return timeCurrentEvent + distribution.sample();
	}

}
