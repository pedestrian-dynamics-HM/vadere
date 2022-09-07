package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

@ModelAttributeClass
public class AttributesSingleTargetStrategy extends Attributes {
	private final int targetID = 0;

	public int getTargetID() {
		return targetID;
	}
}
