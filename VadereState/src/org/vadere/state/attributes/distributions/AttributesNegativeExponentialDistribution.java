package org.vadere.state.attributes.distributions;

/**
 * This is the parameter structure used with a negative exponential distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */
public class AttributesNegativeExponentialDistribution extends AttributesDistribution {
	Double mean = 0.0;

	public void setMean(double mean) {
		this.mean = mean;
	}

	public double getMean() {
		return mean;
	}
}
