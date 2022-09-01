package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.parameter.AttributesBinomialDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "binomial", parameter = AttributesBinomialDistribution.class)
public class BinomialDistribution extends VDistribution<AttributesBinomialDistribution> {
	private Attributes binomialAttributes;
	public BinomialDistribution(AttributesBinomialDistribution parameter, int spawnNumber, RandomGenerator randomGenerator)
	        throws Exception {
		super(parameter, spawnNumber, randomGenerator);
	}

	private org.apache.commons.math3.distribution.BinomialDistribution distribution;
	private int remainingSpawnAgents;
	private int spawnNumber;

	@Override
	protected void setValues(AttributesBinomialDistribution parameter, int spawnNumber, RandomGenerator randomGenerator) {
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

	@Override
	public Attributes getAttributes() {
		return this.binomialAttributes;
	}

	@Override
	public void setAttributes(Attributes attributes) {
		if(attributes instanceof  AttributesBinomialDistribution)
			this.binomialAttributes = attributes;
		else
			throw new IllegalArgumentException();
	}
}
