package org.vadere.state.scenario.distribution.parameter;


/**
 * This is the parameter structure used with an normal distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */

public class NormalParameter {
	public double getMean() {
		return mean;
	}

	public void setMean(double mean) {
		this.mean = mean;
	}

	double mean;

	public double getSd() {
		return sd;
	}

	public void setSd(double sd) {
		this.sd = sd;
	}

	double sd;
}
