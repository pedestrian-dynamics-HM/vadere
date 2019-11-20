package org.vadere.state.attributes.processor;


public class AttributesPedestrianPositionProcessor extends AttributesProcessor {
	private boolean interpolate = true;

	public void setInterpolate(boolean interpolate) {
		checkSealed();
		this.interpolate = interpolate;
	}

	public boolean isInterpolate() {
		return interpolate;
	}
}
