package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 */

public class AttributesAreaSpeedProcessor extends AttributesAreaProcessor {
	private int pedestrianPositionProcessorId;
	private int pedestrianVelocityProcessorId;

	public int getPedestrianPositionProcessorId() {
		return this.pedestrianPositionProcessorId;
	}

	public void setPedestrianPositionProcessorId(int id) {
		checkSealed();
		this.pedestrianPositionProcessorId = id;
	}

	public int getPedestrianVelocityProcessorId() {
		return this.pedestrianVelocityProcessorId;
	}

	public void setPedestrianVelocityProcessorId(int id) {
		checkSealed();
		this.pedestrianVelocityProcessorId = id;
	}
}
