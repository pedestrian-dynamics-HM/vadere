package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.distributions.AttributesSingleSpawnDistribution;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;
import org.vadere.util.Attributes;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "singleSpawn", parameter = AttributesSingleSpawnDistribution.class)
public class SingleSpawnDistribution extends VDistribution<AttributesSingleSpawnDistribution> {
	private Attributes singlespawnAttributes;

	public SingleSpawnDistribution(){
		// Do not remove this constructor. It is us used through reflection.
		super();
		this.singlespawnAttributes = new AttributesSingleSpawnDistribution();
	}
	public SingleSpawnDistribution(AttributesSingleSpawnDistribution parameter,RandomGenerator unused)
	        throws Exception {
		super(parameter, unused);
	}

	@Override
	protected void setValues(AttributesSingleSpawnDistribution parameter, RandomGenerator unused) throws Exception {
		this.singlespawnAttributes = parameter;
	}

	@Override
	public double getNextSpawnTime(double timeCurrentEvent) {
		var attribs = (AttributesSingleSpawnDistribution)getAttributes();
		return attribs.getSpawnTime();
	}

}
