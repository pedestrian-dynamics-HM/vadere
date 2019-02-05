package org.vadere.state.attributes.processor;

import org.vadere.util.geometry.shapes.VRectangle;

/**
 * @author Benedikt Zoennchen
 */
public class AttributesCrossingTimeProcessor extends AttributesAreaProcessor {
	private VRectangle waitingArea = new VRectangle(0, 0, 1, 1);

	public VRectangle getWaitingArea() {
		return waitingArea;
	}

	public void setWaitingArea(VRectangle waitingArea) {
		checkSealed();
		this.waitingArea = waitingArea;
	}
}
