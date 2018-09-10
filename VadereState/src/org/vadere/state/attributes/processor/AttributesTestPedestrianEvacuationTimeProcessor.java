package org.vadere.state.attributes.processor;

import org.jetbrains.annotations.NotNull;

/**
 * @author Benedikt Zoennchen
 */
public class AttributesTestPedestrianEvacuationTimeProcessor extends AttributesTestProcessor {
	private int pedestrianEvacuationTimeProcessorId;
	private Double maximalEvacuationTime = Double.POSITIVE_INFINITY;
	private Double minimalEvacuationTime = 0.0;

	public int getPedestrianEvacuationTimeProcessorId() {
		return this.pedestrianEvacuationTimeProcessorId;
	}

	public double getMaximalEvacuationTime() {
		return maximalEvacuationTime;
	}

	public double getMinimalEvacuationTime() {
		return minimalEvacuationTime;
	}

	public void setPedestrianEvacuationTimeProcessorId(final int pedestrianEvacuationTimeProcessorId) {
		checkSealed();
		this.pedestrianEvacuationTimeProcessorId = pedestrianEvacuationTimeProcessorId;
	}

	public void setMaximalEvacuationTime(@NotNull final Double maximalEvacuationTime) {
		checkSealed();
		this.maximalEvacuationTime = maximalEvacuationTime;
	}

	public void setMinimalEvacuationTime(@NotNull final Double minimalEvacuationTime) {
		checkSealed();
		this.minimalEvacuationTime = minimalEvacuationTime;
	}
}
