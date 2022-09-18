package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.distributions.AttributesConstantDistribution;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */

@RegisterDistribution(name = "constant", parameter = AttributesConstantDistribution.class)
public class ConstantDistribution extends VDistribution<AttributesConstantDistribution> {


	@Override
	protected void setValues(AttributesConstantDistribution parameter, RandomGenerator unused) throws Exception {
		this.attributes = parameter;
	}

	public ConstantDistribution(AttributesConstantDistribution parameter,RandomGenerator unused) throws Exception {
		super(parameter,unused);
	}

	@Override
	public double getNextSample(double timeCurrentEvent) {
		// always add a constant value to the 'value'
		var attribs = (AttributesConstantDistribution)this.getAttributes();
		return timeCurrentEvent + attribs.getUpdateFrequency();
	}
}
