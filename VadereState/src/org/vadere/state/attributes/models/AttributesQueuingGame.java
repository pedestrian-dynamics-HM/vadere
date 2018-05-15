package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

@ModelAttributeClass
public class AttributesQueuingGame extends Attributes {

	private AttributesFloorField queuingFloorField;
	private AttributesFloorField navigationFloorField;
	private double competitiveProbability;
	private double expectedGentleTimeInSec;
	private double expectedCompetitiveTimeInSec;

	public AttributesFloorField getNavigationFloorField() {
		return navigationFloorField;
	}

	public AttributesFloorField getQueuingFloorField() {
		return queuingFloorField;
	}

	public double getCompetitiveProbability() {
		return competitiveProbability;
	}

	public double getExpectedGentleTimeInSec() {
		return expectedGentleTimeInSec;
	}

	public double getExpectedCompetitiveTimeInSec() {
		return expectedCompetitiveTimeInSec;
	}
}
