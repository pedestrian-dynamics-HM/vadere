package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.distributions.AttributesConstantDistribution;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;
import org.vadere.util.Attributes;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */

@RegisterDistribution(name = "constant", parameter = AttributesConstantDistribution.class)
public class ConstantDistribution extends VDistribution<AttributesConstantDistribution> {

	private Attributes constantAttributes;

	public ConstantDistribution(){
		// Do not remove this constructor. It is us used through reflection.
		super();
		this.constantAttributes = new AttributesConstantDistribution();
	}

	@Override
	protected void setValues(AttributesConstantDistribution parameter, RandomGenerator unused) throws Exception {
		this.constantAttributes = parameter;
	}

	public ConstantDistribution(AttributesConstantDistribution parameter,RandomGenerator unused) throws Exception {
		super(parameter,unused);
	}

	@Override
	public double getNextSpawnTime(double timeCurrentEvent) {
		// always add a constant value to the 'value'
		var attribs = (AttributesConstantDistribution)this.getAttributes();
		return timeCurrentEvent + attribs.getUpdateFrequency();
	}
}
