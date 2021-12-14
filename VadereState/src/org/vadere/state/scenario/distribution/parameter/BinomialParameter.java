package org.vadere.state.scenario.distribution.parameter;


/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */


public class BinomialParameter {

	private int trials;
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
