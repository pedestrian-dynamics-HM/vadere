package org.vadere.state.scenario.distribution.parameter;


/**
 * @author Lukas Gradl (lgradl@hm.edu)
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
