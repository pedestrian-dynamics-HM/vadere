package org.vadere.state.scenario.distribution.parameter;

/**
 * This is the parameter structure used with a negative exponential distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */
public class NegativeExponentialParameter {
	double mean;

	public void setMean(double mean) {
		this.mean = mean;
	}

	public double getMean() {
		return mean;
	}
}
