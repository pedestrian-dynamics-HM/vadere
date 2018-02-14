package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 */

public class AttributesPedestrianEvacuationTimeProcessor extends AttributesProcessor {
	private int pedestrianStartTimeProcessorId;

	public int getPedestrianStartTimeProcessorId() {
		return this.pedestrianStartTimeProcessorId;
	}

	public void setPedestrianStartTimeProcessorId(int id) {
		checkSealed();
		this.pedestrianStartTimeProcessorId = id;
	}
}
