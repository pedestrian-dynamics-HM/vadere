package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.scenario.distribution.VadereDistribution;
import org.vadere.state.scenario.distribution.parameter.SingleEventParameter;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "singleEvent", parameter = SingleEventParameter.class)
public class SingleEventDistribution extends VadereDistribution<SingleEventParameter> {
	private int spawnNumber;
	private double eventTime;
	private int remainingSpawnAgents;

	public SingleEventDistribution(SingleEventParameter parameter, int spawnNumber, RandomGenerator unused)
	        throws Exception {
		super(parameter, spawnNumber, unused);
	}

	@Override
	protected void setValues(SingleEventParameter parameter, int spawnNumber, RandomGenerator unused) throws Exception {
		this.eventTime = parameter.getEventTime();
		this.spawnNumber = parameter.getSpawnNumber();
	}

	@Override
	public int getSpawnNumber(double timeCurrentEvent) {
		return this.spawnNumber;
	}

	@Override
	public double getNextSample(double timeCurrentEvent) {
		return eventTime;
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
