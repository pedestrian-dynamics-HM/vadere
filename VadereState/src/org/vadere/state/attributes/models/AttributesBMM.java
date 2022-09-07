package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

@ModelAttributeClass
public class AttributesBMM extends Attributes {

	private final double reachedStepTolerance = 0.1;
	private final double acceleration = 1.0;
	private final boolean stepwiseDecisions = true;

	public double getReachedStepTolerance() {
		return reachedStepTolerance;
	}

	public double getAcceleration() {
		return acceleration;
	}

	public boolean isStepwiseDecisions() {
		return stepwiseDecisions;
	}

}
