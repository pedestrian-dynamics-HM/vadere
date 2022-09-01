package org.vadere.state.scenario.distribution.parameter;


import org.vadere.state.attributes.distributions.AttributesDistribution;

/**
 * @author Lukas Gradl (lgradl@hm.edu)
 */

public class AttributesNormalDistribution extends AttributesDistribution {
	public double getMean() {
		return mean;
	}

	public void setMean(double mean) {
		this.mean = mean;
	}

	Double mean;

	public double getSd() {
		return sd;
	}

	public void setSd(double sd) {
		this.sd = sd;
	}

	Double sd;
}
