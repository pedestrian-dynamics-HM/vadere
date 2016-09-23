package org.vadere.simulator.distribution;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.util.Pair;
import org.junit.Test;
import org.vadere.util.test.StatisticalTestCase;

public class TestEnumeratedDistribution {

	private static final double[] TEST_FRACTIONS = { 1, 3 };
	private static final int[] TEST_DATA = { 0, 1 };
	private static final int SAMPLE_COUNT = 1000;

	@StatisticalTestCase
	@Test
	public void testEnumeratedIntegerDistributionWithFractions() {
		EnumeratedIntegerDistribution d = new EnumeratedIntegerDistribution(TEST_DATA, TEST_FRACTIONS);
		Integer[] values = ArrayUtils.toObject(d.sample(SAMPLE_COUNT));
		List<Integer> result = Arrays.asList(values);
		assertEquals(0.25, (double) Collections.frequency(result, 0) / SAMPLE_COUNT, 0.05);
		assertEquals(0.75, (double) Collections.frequency(result, 1) / SAMPLE_COUNT, 0.05);
	}

	@StatisticalTestCase
	@Test
	public void testEnumeratedDistributionWithFractions() {
		List<Pair<Integer, Double>> list = new ArrayList<>(2);
		for (int i = 0; i < TEST_DATA.length; i++) {
			list.add(new Pair<>(TEST_DATA[i], TEST_FRACTIONS[i]));
		}
		EnumeratedDistribution<Integer> d = new EnumeratedDistribution<>(list);
		List<Integer> result = new ArrayList<>(SAMPLE_COUNT);
		for (Object x : d.sample(SAMPLE_COUNT)) {
			result.add((Integer) x);
		}
		assertEquals(0.25, (double) Collections.frequency(result, 0) / SAMPLE_COUNT, 0.05);
		assertEquals(0.75, (double) Collections.frequency(result, 1) / SAMPLE_COUNT, 0.05);
	}

}
