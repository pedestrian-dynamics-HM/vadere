package org.vadere.simulator.models.groups;

import java.util.List;
import java.util.Random;

public class GroupSizeDeterminatorRandom implements GroupSizeDeterminator {

	private double[] probabilities;
	private final Random random;

	public GroupSizeDeterminatorRandom(List<Double> fractions, Random random) {

		probabilities = new double[fractions.size()];
		double sum = 0;

		for (double i : fractions) {
			sum += i;
		}

		if (sum == 0) {
			throw new IllegalArgumentException(
					"Sum of group size fractions must not be 0.");
		}

		for (int i = 0; i < fractions.size(); i++) {
			probabilities[i] = fractions.get(i) / sum;
		}

		this.random = new Random();
	}

	@Override
	public int nextGroupSize() {
		int result = 1;

		double rand = random.nextDouble();
		double sum = 0;

		for (double prob : probabilities) {
			sum += prob;
			if (rand <= sum) {
				break;
			} else {
				result++;
			}
		}
		return result;
	}

}
