package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.util.Attributes;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.attributes.distributions.AttributesEmpiricalDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "empirical", parameter = AttributesEmpiricalDistribution.class)
public class EmpiricalDistribution extends VDistribution<AttributesEmpiricalDistribution> {
	private Attributes empiricalAttributes;
	private org.apache.commons.math3.random.EmpiricalDistribution distribution;
	/*
	private int spawnNumber;
	private int remainingSpawnAgents;
	 */

	public EmpiricalDistribution(){
		// Do not remove this constructor. It is us used through reflection.
		super();
		this.empiricalAttributes = new AttributesEmpiricalDistribution();
	}
	public EmpiricalDistribution(AttributesEmpiricalDistribution parameter, int spawnNumber, RandomGenerator randomGenerator)
	        throws Exception {
		super(parameter, spawnNumber, randomGenerator);
	}

	@Override
	protected void setValues(AttributesEmpiricalDistribution parameter, int spawnNumber, RandomGenerator randomGenerator) {
		distribution = new org.apache.commons.math3.random.EmpiricalDistribution(randomGenerator);
		//distribution.load(parameter.getValues());
		//this.spawnNumber = spawnNumber;

	}
	@Override
	public double getNextSpawnTime(double timeCurrentEvent) {
		return timeCurrentEvent + distribution.sample();
	}
	/*
        @Override
        public int getSpawnNumber(double timeCurrentEvent) {
            return spawnNumber;
        }
    */
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
		return this.empiricalAttributes;
	}

	@Override
	public void setAttributes(Attributes attributes) {
		if(attributes instanceof AttributesEmpiricalDistribution)
			this.empiricalAttributes = attributes;
		else
			throw new IllegalArgumentException();
	}
}
