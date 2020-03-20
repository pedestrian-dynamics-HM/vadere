package org.vadere.state.attributes.processor;

public class AttributesMeshDensityCountingProcessor extends AttributesProcessor {
	private int meshProcessorId;

	public int getMeshProcessorId() {
		return meshProcessorId;
	}

	public void setMeshProcessorId(int meshProcessorId) {
		checkSealed();
		this.meshProcessorId = meshProcessorId;
	}
}
