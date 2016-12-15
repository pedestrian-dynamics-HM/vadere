package org.vadere.simulator.models.queuing;

import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.scenario.Target;

public class TargetQueue extends Target {

	public TargetQueue(final AttributesTarget attributes) {
		super(attributes);
	}

	@Override
	public boolean isMovingTarget() {
		return true;
	}

	@Override
	public TargetQueue clone() {
		throw new RuntimeException("clone is not supported for TargetQueue; it seems hard to implement.");
	}

}
