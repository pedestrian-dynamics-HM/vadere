package org.vadere.state.attributes.processor;

public class AttributesFundamentalDiagramAProcessor extends AttributesProcessor {
	private int pedestrianLineCrossProcessorId;
	private int pedestrianVelocityProcessorId;
	private double deltaTime;

	public double getDeltaTime() {
		return deltaTime;
	}

	public int getPedestrianLineCrossProcessorId() {
		return pedestrianLineCrossProcessorId;
	}

	public int getPedestrianVelocityProcessorId() {
		return pedestrianVelocityProcessorId;
	}

	public void setDeltaTime(double deltaTime) {
		checkSealed();
		this.deltaTime = deltaTime;
	}

	public void setPedestrianLineCrossProcessorId(int pedestrianLineCrossProcessorId) {
		checkSealed();
		this.pedestrianLineCrossProcessorId = pedestrianLineCrossProcessorId;
	}

	public void setPedestrianVelocityProcessorId(int pedestrianVelocityProcessorId) {
		checkSealed();
		this.pedestrianVelocityProcessorId = pedestrianVelocityProcessorId;
	}
}
