package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.distributions.AttributesPoissonDistribution;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;
import org.vadere.state.attributes.Attributes;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "poisson", parameter = AttributesPoissonDistribution.class)
public class PoissonDistribution extends VDistribution<AttributesPoissonDistribution> {
	private ExponentialDistribution distribution;

	private Attributes poissonAttributes;
	public PoissonDistribution(){
		// Do not remove this constructor. It is us used through reflection.
		super();
		this.poissonAttributes = new AttributesPoissonDistribution();
	}
	public PoissonDistribution(AttributesPoissonDistribution parameter, RandomGenerator randomGenerator)
	        throws Exception {
		super(parameter, randomGenerator);
	}

	@Override
	protected void setValues(AttributesPoissonDistribution parameter, RandomGenerator randomGenerator)
	        throws Exception {
		distribution = new ExponentialDistribution(randomGenerator, 1 / parameter.getNumberPedsPerSecond());
		this.poissonAttributes = parameter;
	}

	@Override
	public double getNextSpawnTime(double timeCurrentEvent) {
		return timeCurrentEvent + distribution.sample();
	}

}
