package org.vadere.simulator.models.groups;

import java.util.Random;

/**
 * The Distribution of the generated group sizes is a zero truncated Poisson
 * distribution with mean lambda.
 * 
 */
public class GroupSizeDeterminatorPoisson implements GroupSizeDeterminator {

	private double lambda;
	private final Random random;

	public GroupSizeDeterminatorPoisson(double lambda, Random random) {

		if (lambda < 1) {
			throw new IllegalArgumentException(
					"lambda must not be smaller than 1.");
		}

		this.lambda = lambda;

		this.random = random;
	}

	@Override
	public int nextGroupSize() {
		int x = 0;
		double t = 0.0;

		while (t < 1.0) {
			t -= Math.log(random.nextDouble()) / (lambda - 1);
			x++;
		}

		return x;
	}
}
