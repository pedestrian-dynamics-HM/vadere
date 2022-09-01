package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.attributes.distributions.AttributesSingleSpawnDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "singleSpawn", parameter = AttributesSingleSpawnDistribution.class)
public class SingleSpawnDistribution extends VDistribution<AttributesSingleSpawnDistribution> {
	private Attributes singelspawnAttributes;
	private int spawnNumber;
	private double spawnTime;
	private int remainingSpawnAgents;

	public SingleSpawnDistribution(AttributesSingleSpawnDistribution parameter, int spawnNumber, RandomGenerator unused)
	        throws Exception {
		super(parameter, spawnNumber, unused);
	}

	@Override
	protected void setValues(AttributesSingleSpawnDistribution parameter, int spawnNumber, RandomGenerator unused) throws Exception {
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

	@Override
	public Attributes getAttributes() {
		return this.singelspawnAttributes;
	}

	@Override
	public void setAttributes(Attributes attributes) {
		if(attributes instanceof AttributesSingleSpawnDistribution)
			this.singelspawnAttributes = attributes;
		else
			throw new IllegalArgumentException();
	}
}
