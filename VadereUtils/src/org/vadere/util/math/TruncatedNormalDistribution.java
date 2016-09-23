package org.vadere.util.math;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;

public class TruncatedNormalDistribution extends NormalDistribution {

	private static final long serialVersionUID = 1L;
	
	private double min;
	private double max;

	public TruncatedNormalDistribution(RandomGenerator rng, double mean, double standardDeviation, double min,
			double max) {
		super(rng, mean, standardDeviation);
		if (max <= min)
			throw new IllegalArgumentException("Parameter min must be less than max.");
		this.min = min;
		this.max = max;
	}

	@Override
	public double sample() {
		double sample = super.sample();
		while (sample < min || sample > max) {
			sample = super.sample();
		}
		return sample;
	}

}