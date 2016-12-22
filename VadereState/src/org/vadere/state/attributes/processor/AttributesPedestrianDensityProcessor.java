package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 *
 */

public class AttributesPedestrianDensityProcessor extends AttributesProcessor {
	private int pedestrianPositionProcessorId;

	public int getPedestrianPositionProcessorId() {
		return this.pedestrianPositionProcessorId;
	}

	public void setPedestrianPositionProcessorId(int pedestrianPositionProcessorId) {
		checkSealed();
		this.pedestrianPositionProcessorId = pedestrianPositionProcessorId;
	}
}
