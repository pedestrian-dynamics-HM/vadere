package org.vadere.state.attributes.processor;


/**
 * @author Benedikt Zoennchen
 */
public class AttributesTestNumberOverlapsProcessor extends AttributesTestProcessor {
	private int numberOverlapsProcessorId;
	private int maxOverlaps = 0;

	public int getNumberOverlapsProcessorId() {
		return numberOverlapsProcessorId;
	}

	public int getMaxOverlaps() {
		return maxOverlaps;
	}

	public void setNumberOverlapsProcessorId(int numberOverlapsProcessorId) {
		checkSealed();
		this.numberOverlapsProcessorId = numberOverlapsProcessorId;
	}

	public void setMaxOverlaps(int maxOverlaps) {
		checkSealed();
		this.maxOverlaps = maxOverlaps;
	}
}
