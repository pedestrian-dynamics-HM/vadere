package org.vadere.state.scenario.distribution.parameter;


/**
 * @author Lukas Gradl (lgradl@hm.edu)
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
