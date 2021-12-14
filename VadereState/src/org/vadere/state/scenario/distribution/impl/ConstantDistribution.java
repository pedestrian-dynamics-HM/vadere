package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.scenario.distribution.VadereDistribution;
import org.vadere.state.scenario.distribution.parameter.ConstantParameter;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "constant", parameter = ConstantParameter.class)
public class ConstantDistribution extends VadereDistribution<ConstantParameter> {

	public ConstantDistribution(ConstantParameter parameter, int spawnNumber, RandomGenerator unused) throws Exception {
		super(parameter, spawnNumber, unused);
	}

	private double updateFrequency;
	private int spawnNumber;
	private int remainingSpawnAgents;

	@Override
	protected void setValues(ConstantParameter parameter, int spawnNumber, RandomGenerator unused) {
		this.spawnNumber = spawnNumber;
		this.updateFrequency = parameter.getUpdateFrequency();

	}

	@Override
	public int getSpawnNumber(double timeCurrentEvent) {
		return spawnNumber;
	}

	@Override
	public double getNextSpawnTime(double timeCurrentEvent) {
		// always add a constant value to the 'value'
		return timeCurrentEvent + this.updateFrequency;
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
