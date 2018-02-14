package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 *
 */

public class AttributesPedestrianLastPositionProcessor extends AttributesProcessor {
	private int pedestrianPositionProcessorId;

	public int getPedestrianPositionProcessorId() {
		return this.pedestrianPositionProcessorId;
	}

	public void setPedestrianPositionProcessorId(int id){
		checkSealed();
		this.pedestrianPositionProcessorId = id;
	}
}
