package org.vadere.state.attributes.processors;

import org.vadere.state.attributes.Attributes;
import org.vadere.util.geometry.shapes.VRectangle;

public class AttributesPedestrianWaitingTimeProcessor extends Attributes {

	private VRectangle waitingArea = new VRectangle(0, 0, 1, 1);

	public AttributesPedestrianWaitingTimeProcessor() {}

	public VRectangle getWaitingArea() {
		return waitingArea;
	}
}
