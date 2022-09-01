package org.vadere.state.scenario.distribution.parameter;


import org.vadere.state.attributes.distributions.AttributesDistribution;

/**
 * This is the parameter structure used with a binomial distribution.
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */


public class AttributesBinomialDistribution extends AttributesDistribution {

	private Integer trials;
	private Double p;

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
