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
}
