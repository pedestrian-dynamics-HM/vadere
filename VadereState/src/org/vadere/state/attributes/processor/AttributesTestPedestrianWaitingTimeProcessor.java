package org.vadere.state.attributes.processor;

import org.jetbrains.annotations.NotNull;

/**
 * @author Benedikt Zoennchen
 */
public class AttributesTestPedestrianWaitingTimeProcessor extends AttributesTestProcessor {
	private int pedestrianWaitingTimeProcessorId;
	private Double maximalWaitingTime = Double.POSITIVE_INFINITY;
	private Double minimalWaitingTime = 0.0;

	public int getPedestrianWaitingTimeProcessorId() {
		return pedestrianWaitingTimeProcessorId;
	}

	public double getMaximalWaitingTime() {
		return maximalWaitingTime;
	}

	public double getMinimalWaitingTime() {
		return minimalWaitingTime;
	}

	public void setPedestrianWaitingTimeProcessorId(final int pedestrianWaitingTimeProcessorId) {
		checkSealed();
		this.pedestrianWaitingTimeProcessorId = pedestrianWaitingTimeProcessorId;
	}

	public void setMaximalWaitingTime(@NotNull final Double maximalWaitingTime) {
		checkSealed();
		this.maximalWaitingTime = maximalWaitingTime;
	}

	public void setMinimalWaitingTime(@NotNull final Double minimalWaitingTime) {
		checkSealed();
		this.minimalWaitingTime = minimalWaitingTime;
	}
}
