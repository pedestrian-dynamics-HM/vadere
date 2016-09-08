package org.vadere.state.attributes.processors;

public abstract class AttributesProcessor {
	private int processorId;

	public int getProcessorId() {
		return this.processorId;
	}

	public void setProcessorId(int processorId) {
		this.processorId = processorId;
	}
}
