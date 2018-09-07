package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

@ModelAttributeClass
public class AttributesBMM extends Attributes {

	private double reachedStepTolerance = 0.1;
	private double acceleration = 1.0;
	private boolean stepwiseDecisions = true;

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
