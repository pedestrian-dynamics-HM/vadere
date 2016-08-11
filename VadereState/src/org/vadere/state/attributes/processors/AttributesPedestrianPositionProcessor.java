package org.vadere.state.attributes.processors;

public class AttributesPedestrianPositionProcessor {

	private boolean ignoreEqualPositions = false;

	public AttributesPedestrianPositionProcessor() {}

	public AttributesPedestrianPositionProcessor(final boolean ignoreEqualPositions) {
		this.ignoreEqualPositions = ignoreEqualPositions;
	}

	public boolean isIgnoreEqualPositions() {
		return ignoreEqualPositions;
	}
}
