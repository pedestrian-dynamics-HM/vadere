package org.vadere.state.attributes.exceptions;

import org.vadere.state.attributes.Attributes;

@SuppressWarnings("serial")
public class AttributesNotFoundException extends RuntimeException {
	public AttributesNotFoundException(Class<? extends Attributes> attributesClass) {
		super(attributesClass.getName());
	}
}
