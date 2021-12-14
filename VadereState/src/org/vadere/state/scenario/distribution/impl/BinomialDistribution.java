package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.scenario.distribution.VadereDistribution;
import org.vadere.state.scenario.distribution.parameter.BinomialParameter;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "binomial", parameter = BinomialParameter.class)
public class BinomialDistribution extends VadereDistribution<BinomialParameter> {

	public BinomialDistribution(BinomialParameter parameter, int spawnNumber, RandomGenerator randomGenerator)
	        throws Exception {
		super(parameter, spawnNumber, randomGenerator);
	}

	private org.apache.commons.math3.distribution.BinomialDistribution distribution;
	private int remainingSpawnAgents;
	private int spawnNumber;

	@Override
	protected void setValues(BinomialParameter parameter, int spawnNumber, RandomGenerator randomGenerator) {
		this.distribution = new org.apache.commons.math3.distribution.BinomialDistribution(randomGenerator,
		        parameter.getTrials(), parameter.getP());
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
