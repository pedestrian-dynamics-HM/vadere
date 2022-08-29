package org.vadere.state.scenario.distribution.parameter;


/**
 * This is the parameter structure used with a poisson distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */

public class PoissonParameter {
	public double getNumberPedsPerSecond() {
		return numberPedsPerSecond;
	}

	public void setNumberPedsPerSecond(double numberPedsPerSecond) {
		this.numberPedsPerSecond = numberPedsPerSecond;
	}

	double numberPedsPerSecond;
}
