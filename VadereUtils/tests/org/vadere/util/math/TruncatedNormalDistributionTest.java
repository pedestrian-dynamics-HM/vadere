package org.vadere.util.math;

import static org.junit.Assert.*;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Ignore;
import org.junit.Test;
import org.vadere.util.test.StatisticalTestCase;

public class TruncatedNormalDistributionTest {
	
	private static final int maxIterations = 100;
	private RandomGenerator rng = new JDKRandomGenerator();

	@Test(expected=IllegalArgumentException.class)
	@Ignore
	public void testConstructorMinMaxEquals() {
		final double m = 2;
		new TruncatedNormalDistribution(rng, 0, 1, m, m, maxIterations);
	}

	@Test(expected=IllegalArgumentException.class)
	@Ignore
	public void testConstructorMinGreaterThanMax() {
		new TruncatedNormalDistribution(rng, 0, 1, 2, 1, maxIterations);
	}

	@StatisticalTestCase
	@Test
	@Ignore
	public void testSample() {
		final double min = -1;
		final double max = +1;
		final TruncatedNormalDistribution d = new TruncatedNormalDistribution(rng, 0, 1, min, max, maxIterations);
		for (int i = 0; i < 1000; i++) {
			final double x = d.sample();
			assertTrue(x >= min && x <= max);
		}
	}

	@StatisticalTestCase
	@Test(expected=IllegalArgumentException.class)
	public void testBadMinMax() {
		final double min = -10001;
		final double max = -10000;
		final TruncatedNormalDistribution d = new TruncatedNormalDistribution(rng, 0, 1, min, max, 10);
		d.sample();
	}

}
