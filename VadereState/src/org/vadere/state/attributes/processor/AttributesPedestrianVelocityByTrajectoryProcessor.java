package org.vadere.state.attributes.processor;

/**
 * @author Benedikt Zoennchen
 *
 */
public class AttributesPedestrianVelocityByTrajectoryProcessor extends AttributesProcessor {
	private int pedestrianTrajectoryProcessorId;
	private int backSteps = 1;

	public int getPedestrianTrajectoryProcessorId() {
		return pedestrianTrajectoryProcessorId;
	}

	public void setPedestrianTrajectoryProcessorId(int pedestrianTrajectoryProcessorId) {
		checkSealed();
		this.pedestrianTrajectoryProcessorId = pedestrianTrajectoryProcessorId;
	}

	public int getBackSteps() {
		return this.backSteps;
	}

	public void setBackSteps(int backSteps) {
		checkSealed();
		this.backSteps = backSteps;
	}
}
