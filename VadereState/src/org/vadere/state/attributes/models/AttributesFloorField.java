package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.types.EikonalSolverType;

@ModelAttributeClass
public class AttributesFloorField extends Attributes {

	private EikonalSolverType createMethod = EikonalSolverType.HIGH_ACCURACY_FAST_MARCHING;
	private double potentialFieldResolution = 0.1;
	private double obstacleGridPenalty = 0.1;
	private double targetAttractionStrength = 1.0;
	private AttributesTimeCost timeCostAttributes;

	public AttributesFloorField() {
		timeCostAttributes = new AttributesTimeCost();
	}

	// Getters...
	public EikonalSolverType getCreateMethod() {
		return createMethod;
	}

	public double getPotentialFieldResolution() {
		return potentialFieldResolution;
	}

	public double getObstacleGridPenalty() {
		return obstacleGridPenalty;
	}

	public double getTargetAttractionStrength() {
		return targetAttractionStrength;
	}

	public AttributesTimeCost getTimeCostAttributes() {
		return timeCostAttributes;
	}
}
