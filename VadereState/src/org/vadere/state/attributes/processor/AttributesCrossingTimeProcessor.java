package org.vadere.state.attributes.processor;

/**
 * @author Benedikt Zoennchen
 */
public class AttributesCrossingTimeProcessor extends AttributesAreaProcessor {
	private int waitingAreaId = -1;

	public int getWaitingAreaId() {
		return waitingAreaId;
	}

	public void setWaitingAreaId(int waitingAreaId) {
		checkSealed();
		this.waitingAreaId = waitingAreaId;
	}
}
