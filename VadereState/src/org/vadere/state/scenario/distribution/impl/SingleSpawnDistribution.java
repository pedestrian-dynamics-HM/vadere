package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.util.Attributes;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.attributes.distributions.AttributesSingleSpawnDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "singleSpawn", parameter = AttributesSingleSpawnDistribution.class)
public class SingleSpawnDistribution extends VDistribution<AttributesSingleSpawnDistribution> {
	private Attributes singelspawnAttributes;

	private double spawnTime;

	public SingleSpawnDistribution(){
		// Do not remove this constructor. It is us used through reflection.
		super();
		this.singelspawnAttributes = new AttributesSingleSpawnDistribution();
	}
	public SingleSpawnDistribution(AttributesSingleSpawnDistribution parameter, int spawnNumber, RandomGenerator unused)
	        throws Exception {
		super(parameter, unused);
	}

	@Override
	protected void setValues(AttributesSingleSpawnDistribution parameter, RandomGenerator unused) throws Exception {
		this.spawnTime = parameter.getSpawnTime();
	}

	@Override
	public double getNextSpawnTime(double timeCurrentEvent) {
		return spawnTime;
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
