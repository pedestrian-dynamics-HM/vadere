package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.util.Attributes;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.attributes.distributions.AttributesPoissonDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "poisson", parameter = AttributesPoissonDistribution.class)
public class PoissonDistribution extends VDistribution<AttributesPoissonDistribution> {
	private ExponentialDistribution distribution;
	/*
	private int spawnNumber;
	private int remainingSpawnAgents;
	*/

	private Attributes poissonAttributes;
	public PoissonDistribution(){
		// Do not remove this constructor. It is us used through reflection.
		super();
		this.poissonAttributes = new AttributesPoissonDistribution();
	}
	public PoissonDistribution(AttributesPoissonDistribution parameter, int spawnNumber, RandomGenerator randomGenerator)
	        throws Exception {
		super(parameter, randomGenerator);
	}

	@Override
	protected void setValues(AttributesPoissonDistribution parameter, RandomGenerator randomGenerator)
	        throws Exception {
		distribution = new ExponentialDistribution(randomGenerator, 1 / parameter.getNumberPedsPerSecond());
		//this.spawnNumber = spawnNumber;
	}
/*
	@Override
	public int getSpawnNumber(double timeCurrentEvent) {
		return spawnNumber;
	}
*/
	@Override
	public double getNextSpawnTime(double timeCurrentEvent) {
		return timeCurrentEvent + distribution.sample();
	}
/*
	@Override
	public int getRemainingSpawnAgents() {
		return remainingSpawnAgents;
	}

	@Override
	public void setRemainingSpawnAgents(int remainingSpawnAgents) {
		this.remainingSpawnAgents = remainingSpawnAgents;
	}
*/
	@Override
	public Attributes getAttributes() {
		return this.poissonAttributes;
	}

	@Override
	public void setAttributes(Attributes attributes) {
		if(attributes instanceof AttributesPoissonDistribution)
			this.poissonAttributes = attributes;
		else
			throw new IllegalArgumentException();
	}
}
