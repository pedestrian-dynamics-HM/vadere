package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.attributes.distributions.AttributesConstantDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "constant", parameter = AttributesConstantDistribution.class)
public class ConstantDistribution extends VDistribution<AttributesConstantDistribution> {

	private Attributes constantAttributes;

	public ConstantDistribution(AttributesConstantDistribution parameter, int spawnNumber, RandomGenerator unused) throws Exception {
		super(parameter,spawnNumber,unused);
	}
	public ConstantDistribution(){
		super();
		this.constantAttributes = new AttributesConstantDistribution();
	}

	private double updateFrequency;
	private int spawnNumber;
	private int remainingSpawnAgents;

	@Override
	protected void setValues(AttributesConstantDistribution attributes, int spawnNumber, RandomGenerator unused) {
		this.constantAttributes = attributes;
		this.spawnNumber = spawnNumber;
		this.updateFrequency = attributes.getUpdateFrequency();

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

	@Override
	public Attributes getAttributes() {
		return this.constantAttributes;
	}

	@Override
	public void setAttributes(Attributes attributes) {
		if(attributes instanceof AttributesConstantDistribution)
			this.constantAttributes = attributes;
		else
			throw new IllegalArgumentException();
	}
}
