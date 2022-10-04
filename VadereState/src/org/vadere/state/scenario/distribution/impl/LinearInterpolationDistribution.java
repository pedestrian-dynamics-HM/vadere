package org.vadere.state.scenario.distribution.impl;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.distributions.AttributesLinearInterpolationDistribution;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.distribution.registry.RegisterDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu),
 *         Daniel Lehmberg
 */
@RegisterDistribution(name = "linearInterpolation", parameter = AttributesLinearInterpolationDistribution.class)
public class LinearInterpolationDistribution extends VDistribution<AttributesLinearInterpolationDistribution> {

	public LinearInterpolationDistribution(AttributesLinearInterpolationDistribution parameter,
										   RandomGenerator randomGenerator) throws Exception {
		super(parameter, randomGenerator);
	}

	@Override
	protected void setValues(AttributesLinearInterpolationDistribution parameter, RandomGenerator randomGenerator) {
		// Most correctness is checked in LinearInterpolator
		// double[] xValues = {0., 200., 400, 600, 700, 800};
		// double[] yValues = {3., 8., 2., 10, 0, 0};

		double[] xValues = { 0, 200, 400, 600, 800, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400 };
		double[] yValues = { -1., 8., -1., 8., -1.0, 8., -1, 8, -1, 8, -1, 8, -1 };

		for (double d : yValues) {
			if (d < -1) {
				throw new IllegalArgumentException("No negative values are allowed for yValues. Got " + d);
			}
		}
	}
	@Override
	public double getNextSample(double timeCurrentEvent) {
		var attribs = (AttributesLinearInterpolationDistribution)getAttributes();
		return timeCurrentEvent + attribs.getSpawnFrequency();
	}

}
