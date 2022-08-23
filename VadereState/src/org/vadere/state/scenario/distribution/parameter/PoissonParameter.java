package org.vadere.state.scenario.distribution.parameter;


/**
 * This is the parameter structure used with a poisson distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */

public class PoissonParameter {
	public double getOccurrencesPerSecond() {
		return occurrencesPerSecond;
	}

	public void setOccurrencesPerSecond(double occurrencesPerSecond) {
		this.occurrencesPerSecond = occurrencesPerSecond;
	}

	double occurrencesPerSecond;
}
