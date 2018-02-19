package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 */

public class AttributesMeanPedestrianEvacuationTimeProcessor extends AttributesProcessor {
	private int pedestrianEvacuationTimeProcessorId;

	public int getPedestrianEvacuationTimeProcessorId() {
		return this.pedestrianEvacuationTimeProcessorId;
	}

	public void setPedestrianEvacuationTimeProcessorId(int id) {
		checkSealed();
		this.pedestrianEvacuationTimeProcessorId = id;
	}
}
