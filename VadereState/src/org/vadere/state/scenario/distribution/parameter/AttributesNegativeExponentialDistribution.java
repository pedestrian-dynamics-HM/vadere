package org.vadere.state.scenario.distribution.parameter;

import org.vadere.state.attributes.distributions.AttributesDistribution;

/**
 * @author Lukas Gradl (lgradl@hm.edu)
 */
public class AttributesNegativeExponentialDistribution extends AttributesDistribution {
	Double mean;

	public void setMean(double mean) {
		this.mean = mean;
	}

	public double getMean() {
		return mean;
	}
}
