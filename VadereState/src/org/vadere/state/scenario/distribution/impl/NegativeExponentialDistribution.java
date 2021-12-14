package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.scenario.distribution.VadereDistribution;
import org.vadere.state.scenario.distribution.parameter.NegativeExponentialParameter;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "negativeExponential", parameter = NegativeExponentialParameter.class)
public class NegativeExponentialDistribution extends VadereDistribution<NegativeExponentialParameter> {

	private ExponentialDistribution distribution;
	private int spawnNumber;
	private int remainingSpawnAgents;

	public NegativeExponentialDistribution(NegativeExponentialParameter parameter, int spawnNumber,
	        RandomGenerator randomGenerator) throws Exception {
		super(parameter, spawnNumber, randomGenerator);
	}

	@Override
	protected void setValues(NegativeExponentialParameter parameter, int spawnNumber, RandomGenerator randomGenerator)
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
}
