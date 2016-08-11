package org.vadere.state.scenario;

import org.apache.commons.math3.distribution.ConstantRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * "Constant" distribution for <code>interSpawnTimeDistribution</code> of
 * {@link org.vadere.state.attributes.scenario.AttributesSource}.
 * 
 */
public class ConstantDistribution extends ConstantRealDistribution {
	private static final long serialVersionUID = 1L;

	/** Uniform constructor interface: RandomGenerator unusedRng, double... distributionParams */
	public ConstantDistribution(RandomGenerator unusedRng, double value) {
		super(value);
	}
}
