package org.vadere.state.attributes.distributions;


import org.vadere.util.reflection.VadereAttribute;

/**
 * This is the parameter structure used with a binomial distribution.
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */

public class AttributesBinomialDistribution extends AttributesDistribution {
	@VadereAttribute
	private Integer trials = 0;
	@VadereAttribute
	private Double p = 0.0;

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
