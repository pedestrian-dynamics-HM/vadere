package org.vadere.simulator.projects.dataprocessing_mtp;

public class AttributesVelocityProcessor extends AttributesProcessor {
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
