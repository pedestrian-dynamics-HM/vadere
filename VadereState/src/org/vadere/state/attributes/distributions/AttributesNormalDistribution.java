package org.vadere.state.attributes.distributions;


/**
 * This is the parameter structure used with an normal distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */

public class AttributesNormalDistribution extends AttributesDistribution {
	public double getMean() {
		return mean;
	}

	public void setMean(double mean) {
		this.mean = mean;
	}

	Double mean = 0.0;

	public double getSd() {
		return sd;
	}

	public void setSd(double sd) {
		this.sd = sd;
	}

	Double sd  = 0.0;
}
