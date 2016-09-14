package org.vadere.simulator.models.groups;

import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

public class GroupSizeDeterminatorRandom implements GroupSizeDeterminator {

	private EnumeratedIntegerDistribution distribution;

	public GroupSizeDeterminatorRandom(List<Double> fractions, Random random) {
		
		// It's not clear from the API doc if the EnumeratedIntegerDistribution normalizes fractions.
		// If it does, most of the following is unnecessary.

		double sum = 0;
		for (double i : fractions) {
			sum += i;
		}

		if (sum == 0) {
			throw new IllegalArgumentException("Sum of group size fractions must not be 0.");
		}

		final int[] groupSizeValues = new int[fractions.size()];
		final double[] probabilities = new double[fractions.size()];

		for (int i = 0; i < fractions.size(); i++) {
			groupSizeValues[i] = i + 1;
			probabilities[i] = fractions.get(i) / sum;
		}
		
		final RandomGenerator rng = new JDKRandomGenerator(random.nextInt());
		distribution = new EnumeratedIntegerDistribution(rng, groupSizeValues, probabilities);
	}

	@Override
	public int nextGroupSize() {
		return distribution.sample();
	}

}
