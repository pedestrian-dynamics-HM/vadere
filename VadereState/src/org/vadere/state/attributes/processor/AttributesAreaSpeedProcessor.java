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


    public int getPedestrianVelocityProcessorId() {
        return this.pedestrianVelocityProcessorId;
    }

    public void setPedestrianPositionProcessorId(int pedestrianPositionProcessorId) {
        checkSealed();
        this.pedestrianPositionProcessorId = pedestrianPositionProcessorId;
    }

    public void setPedestrianVelocityProcessorId(int pedestrianVelocityProcessorId) {
        checkSealed();
        this.pedestrianVelocityProcessorId = pedestrianVelocityProcessorId;
    }
}
