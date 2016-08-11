package org.vadere.state.attributes.processors;

import org.vadere.state.attributes.Attributes;

public class AttributesVelocityProcessor extends Attributes {

	private double maxAcceptedVelocity = 3.0;

	private int backSteps = 1;

	private boolean onlyXDirection = false;

	public double getMaxAcceptedVelocity() {
		return maxAcceptedVelocity;
	}

	public int getBackSteps() {
		return backSteps;
	}

	public boolean isOnlyXDirection() {
		return onlyXDirection;
	}
}
