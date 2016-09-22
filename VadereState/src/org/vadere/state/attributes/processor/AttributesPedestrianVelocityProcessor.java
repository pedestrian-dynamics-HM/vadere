package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 *
 */

public class AttributesPedestrianVelocityProcessor extends AttributesProcessor {
	private int pedestrianPositionProcessorId;
	private int backSteps = 1;

	public int getPedestrianPositionProcessorId() {
		return this.pedestrianPositionProcessorId;
	}

	public void setPedestrianPositionProcessorId(int pedestrianPositionProcessorId) {
		this.pedestrianPositionProcessorId = pedestrianPositionProcessorId;
	}

	public int getBackSteps() {
		return this.backSteps;
	}

	public void setBackSteps(int backSteps) {
		this.backSteps = backSteps;
	}
}
