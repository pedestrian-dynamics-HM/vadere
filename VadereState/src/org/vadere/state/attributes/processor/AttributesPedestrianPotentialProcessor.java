package org.vadere.state.attributes.processor;

import org.jetbrains.annotations.NotNull;

public class AttributesPedestrianPotentialProcessor extends AttributesProcessor {
	private int pedestrianPositionProcessorId;

	/**
	 * If this value is -1 then the targetId of the agent's target will be used otherwise this value will be used.
	 */
	private int targetId = -1;
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

	public int getTargetId() {
		return targetId;
	}

	public void setTargetId(int targetId) {
		checkSealed();
		this.targetId = targetId;
	}

	public enum PotentialType {
		TARGET, OBSTACLE, PEDESTRIAN, ALL;
	}
}
