package org.vadere.state.scenario.distribution.parameter;


/**
 * This is the parameter structure used with a binomial distribution.
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */
public class BinomialParameter {

	/**
	 * The number of trials
	 */
	private int trials;

	/**
	 * The probability of success.
	 */
	private double p;

	public int getTrials() {
		return trials;
	}

	public void setTrials(int trials) {
		this.trials = trials;
	}

	public double getP() {
		return p;
	}

	public void setP(double p) {
		this.p = p;
	}
}
