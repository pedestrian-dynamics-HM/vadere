package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.distributions.AttributesSingleSpawnDistribution;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
@RegisterDistribution(name = "singleSpawn", parameter = AttributesSingleSpawnDistribution.class)
public class SingleSpawnDistribution extends VDistribution<AttributesSingleSpawnDistribution> {

	public SingleSpawnDistribution(AttributesSingleSpawnDistribution parameter,RandomGenerator unused)
	        throws Exception {
		super(parameter, unused);
	}

	@Override
	protected void setValues(AttributesSingleSpawnDistribution parameter, RandomGenerator unused) throws Exception {
	}

	@Override
	public double getNextSample(double timeCurrentEvent) {
		var attribs = (AttributesSingleSpawnDistribution)getAttributes();
		return attribs.getSpawnTime();
	}

}
