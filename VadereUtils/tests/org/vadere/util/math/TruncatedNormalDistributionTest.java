package org.vadere.util.math;

import static org.junit.Assert.*;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

public class TruncatedNormalDistributionTest {
	
	private RandomGenerator rng = new JDKRandomGenerator();

	@Test(expected=IllegalArgumentException.class)
	public void testSampleMinMaxEquals() {
		final double m = 2;
		new TruncatedNormalDistribution(rng, 0, 1, m, m);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSampleMinGreaterThanMax() {
		new TruncatedNormalDistribution(rng, 0, 1, 2, 1);
	}

	@Test
	public void testTruncatedNormalDistribution() {
		final double min = -1;
		final double max = +1;
		final TruncatedNormalDistribution d = new TruncatedNormalDistribution(rng, 0, 1, min, max);
		for (int i = 0; i < 1000; i++) {
			final double x = d.sample();
			assertTrue(x >= min && x <= max);
		}
	}

}
