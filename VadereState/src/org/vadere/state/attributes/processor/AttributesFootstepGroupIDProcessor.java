package org.vadere.state.attributes.processor;

public class AttributesFootstepGroupIDProcessor extends AttributesProcessor {
	private int pedestrianFootStepProcessorId;

	public int getPedestrianFootStepProcessorId() {
		return pedestrianFootStepProcessorId;
	}

	public void setPedestrianFootStepProcessorId(int pedestrianFootStepProcessorId) {
		checkSealed();
		this.pedestrianFootStepProcessorId = pedestrianFootStepProcessorId;
	}
}
