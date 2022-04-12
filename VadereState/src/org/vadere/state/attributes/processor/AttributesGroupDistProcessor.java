package org.vadere.state.attributes.processor;

/**
 * @author Manuel Hertle
 *
 */

public class AttributesGroupDistProcessor extends AttributesProcessor {
	private int areaGroupMetaDataProcessorId;

	public int getAreaGroupMetaDataProcessorId() {
		return this.areaGroupMetaDataProcessorId;
	}

	public void setAreaGroupMetaDataProcessorId(int areaGroupMetaDataProcessorId) {
		checkSealed();
		this.areaGroupMetaDataProcessorId= areaGroupMetaDataProcessorId;
	}
}
