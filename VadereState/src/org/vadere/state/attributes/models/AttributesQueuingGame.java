package org.vadere.state.attributes.models;

import org.vadere.state.attributes.Attributes;

public class AttributesQueuingGame extends Attributes {

	private AttributesFloorField queuingFloorField;
	private AttributesFloorField navigationFloorField;
	private double competitiveProbability;
	private double expectedGentleTimeInSec;
	private double expectedCompetitiveTimeInSec;

	public AttributesQueuingGame() {}

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
