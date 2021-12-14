package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.scenario.distribution.VadereDistribution;
import org.vadere.state.scenario.distribution.parameter.EmpiricalParameter;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "empirical", parameter = EmpiricalParameter.class)
public class EmpiricalDistribution extends VadereDistribution<EmpiricalParameter> {

	private org.apache.commons.math3.random.EmpiricalDistribution distribution;
	private int spawnNumber;
	private int remainingSpawnAgents;

	public EmpiricalDistribution(EmpiricalParameter parameter, int spawnNumber, RandomGenerator randomGenerator)
	        throws Exception {
		super(parameter, spawnNumber, randomGenerator);
	}

	@Override
	protected void setValues(EmpiricalParameter parameter, int spawnNumber, RandomGenerator randomGenerator) {
		distribution = new org.apache.commons.math3.random.EmpiricalDistribution(randomGenerator);
		distribution.load(parameter.getValues());
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
