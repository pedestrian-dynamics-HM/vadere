package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.scenario.distribution.VadereDistribution;
import org.vadere.state.scenario.distribution.parameter.NormalParameter;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;
import org.vadere.util.math.TruncatedNormalDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "normal", parameter = NormalParameter.class)
public class NormalDistribution extends VadereDistribution<NormalParameter> {
	private TruncatedNormalDistribution distribution; // we should't really go back in time -> cut the dist at 0
	private int spawnNumber;
	private int remainingSpawnAgents;

	public NormalDistribution(NormalParameter parameter, int spawnNumber, RandomGenerator randomGenerator)
	        throws Exception {
		super(parameter, spawnNumber, randomGenerator);
	}

	@Override
	protected void setValues(NormalParameter parameter, int spawnNumber, RandomGenerator randomGenerator)
	        throws Exception {
		distribution = new TruncatedNormalDistribution(randomGenerator, parameter.getMean(), parameter.getSd(), 0,
		        Double.MAX_VALUE, 1000);
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
