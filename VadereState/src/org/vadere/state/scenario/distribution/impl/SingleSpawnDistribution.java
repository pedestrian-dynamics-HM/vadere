package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.scenario.distribution.VadereDistribution;
import org.vadere.state.scenario.distribution.parameter.SingleSpawnParameter;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "singleSpawn", parameter = SingleSpawnParameter.class)
public class SingleSpawnDistribution extends VadereDistribution<SingleSpawnParameter> {
	private int spawnNumber;
	private double spawnTime;
	private int remainingSpawnAgents;

	public SingleSpawnDistribution(SingleSpawnParameter parameter, int spawnNumber, RandomGenerator unused)
	        throws Exception {
		super(parameter, spawnNumber, unused);
	}

	@Override
	protected void setValues(SingleSpawnParameter parameter, int spawnNumber, RandomGenerator unused) throws Exception {
		this.spawnTime = parameter.getSpawnTime();
		this.spawnNumber = parameter.getSpawnNumber();
	}

	@Override
	public int getSpawnNumber(double timeCurrentEvent) {
		return this.spawnNumber;
	}

	@Override
	public double getNextSpawnTime(double timeCurrentEvent) {
		return spawnTime;
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
