package org.vadere.state.attributes.processor;

import org.jetbrains.annotations.NotNull;

/**
 * @author Benedikt Zoennchen
 */
public class AttributesTestEvacuationTimeProcessor extends AttributesTestProcessor {
	private int evacuationTimeProcessorId;
	private Double maximalEvacuationTime = Double.POSITIVE_INFINITY;
	private Double minimalEvacuationTime = 0.0;

	public int getEvacuationTimeProcessorId() {
		return this.evacuationTimeProcessorId;
	}

	public double getMaximalEvacuationTime() {
		return maximalEvacuationTime;
	}

	public double getMinimalEvacuationTime() {
		return minimalEvacuationTime;
	}

	public void setEvacuationTimeProcessorId(final int evacuationTimeProcessorId) {
		checkSealed();
		this.evacuationTimeProcessorId = evacuationTimeProcessorId;
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
