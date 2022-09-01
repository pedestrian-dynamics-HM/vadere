package org.vadere.state.scenario.distribution.parameter;


import org.vadere.state.attributes.distributions.AttributesDistribution;

/**
 * @author Lukas Gradl (lgradl@hm.edu)
 */

public class AttributesPoissonDistribution extends AttributesDistribution {
	public double getNumberPedsPerSecond() {
		return numberPedsPerSecond;
	}

	public void setNumberPedsPerSecond(double numberPedsPerSecond) {
		this.numberPedsPerSecond = numberPedsPerSecond;
	}

	Double numberPedsPerSecond;
}
