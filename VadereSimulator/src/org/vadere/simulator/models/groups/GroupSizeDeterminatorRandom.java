package org.vadere.simulator.models.groups;

import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

public class GroupSizeDeterminatorRandom implements GroupSizeDeterminator {

	private EnumeratedIntegerDistribution distribution;

	public GroupSizeDeterminatorRandom(List<Double> fractions, Random random) {
		final RandomGenerator rng = new JDKRandomGenerator(random.nextInt());
		
		// The EnumeratedIntegerDistribution works with fractions.
		// We don't have to normalize them to probabilities.
		// See unit test TestEnumeratedDistribution.

		final int[] groupSizeValues = new int[fractions.size()];
		final double[] probabilities = new double[fractions.size()];

		for (int i = 0; i < fractions.size(); i++) {
			groupSizeValues[i] = i + 1;
			probabilities[i] = fractions.get(i);
		}
		
		distribution = new EnumeratedIntegerDistribution(rng, groupSizeValues, probabilities);
	}

	@Override
	public int nextGroupSize() {
		return distribution.sample();
	}

}
