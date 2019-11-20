package org.vadere.state.attributes.processor;

import org.jetbrains.annotations.NotNull;

public class AttributesPedestrianPotentialProcessor extends AttributesProcessor {
	private int pedestrianPositionProcessorId;
	private PotentialType type = PotentialType.TARGET;


	public PotentialType getType() {
		return type;
	}

	public int getPedestrianPositionProcessorId() {
		return pedestrianPositionProcessorId;
	}

	public void setPedestrianPositionProcessorId(final int pedestrianPositionProcessorId) {
		checkSealed();
		this.pedestrianPositionProcessorId = pedestrianPositionProcessorId;
	}

	public void setType(@NotNull final  PotentialType type) {
		checkSealed();
		this.type = type;
	}

	public enum PotentialType {
		TARGET, OBSTACLE, PEDESTRIAN, ALL;
	}
}
