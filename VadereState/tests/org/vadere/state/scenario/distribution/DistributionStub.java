package org.vadere.state.scenario.distribution;

import org.apache.commons.math3.random.RandomGenerator;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
public class DistributionStub extends VDistribution<Object> {

	public DistributionStub(Object parameter, int spawnNumber, RandomGenerator randomGenerator) throws Exception {
		super(parameter, spawnNumber, randomGenerator);
	}

	protected void setValues(Object parameter, int spawnNumber, RandomGenerator randomGenerator) throws Exception {
	}

	public int getSpawnNumber(double timeCurrentEvent) {
		return 0;
	}

	public double getNextSpawnTime(double timeCurrentEvent) {
		return 0;
	}

	public int getRemainingSpawnAgents() {
		return 0;
	}

	public void setRemainingSpawnAgents(int remainingAgents) {
	}

}