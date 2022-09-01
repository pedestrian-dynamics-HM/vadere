package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.parameter.AttributesNegativeExponentialDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "negativeExponential", parameter = AttributesNegativeExponentialDistribution.class)
public class NegativeExponentialDistribution extends VDistribution<AttributesNegativeExponentialDistribution> {
	private Attributes exponAttributes;
	private ExponentialDistribution distribution;
	private int spawnNumber;
	private int remainingSpawnAgents;

	public NegativeExponentialDistribution(AttributesNegativeExponentialDistribution parameter, int spawnNumber,
										   RandomGenerator randomGenerator) throws Exception {
		super(parameter, spawnNumber, randomGenerator);
	}

	@Override
	protected void setValues(AttributesNegativeExponentialDistribution parameter, int spawnNumber, RandomGenerator randomGenerator)
	        throws Exception {
		this.distribution = new ExponentialDistribution(randomGenerator, parameter.getMean());
		this.spawnNumber = spawnNumber;
	}

	@Override
	public int getSpawnNumber(double timeCurrentEvent) {
		return spawnNumber;
	}

	@Override
	public double getNextSpawnTime(double timeCurrentEvent) {
		return timeCurrentEvent + distribution.sample();
	}

	@Override
	public int getRemainingSpawnAgents() {
		return remainingSpawnAgents;
	}

	@Override
	public void setRemainingSpawnAgents(int remainingSpawnAgents) {
		this.remainingSpawnAgents = remainingSpawnAgents;
	}

	@Override
	public Attributes getAttributes() {
		return this.exponAttributes;
	}

	@Override
	public void setAttributes(Attributes attributes) {
		if(attributes instanceof AttributesNegativeExponentialDistribution)
			this.exponAttributes = attributes;
		else
			throw new IllegalArgumentException();

	}
}
