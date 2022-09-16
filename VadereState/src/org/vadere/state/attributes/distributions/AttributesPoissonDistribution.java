package org.vadere.state.attributes.distributions;


/**
 * This is the parameter structure used with a poisson distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */

public class AttributesPoissonDistribution extends AttributesDistribution {
	public double getNumberPedsPerSecond() {
		return numberPedsPerSecond;
	}

	public void setNumberPedsPerSecond(double numberPedsPerSecond) {
		this.numberPedsPerSecond = numberPedsPerSecond;
	}

	Double numberPedsPerSecond = 0.0;
}
