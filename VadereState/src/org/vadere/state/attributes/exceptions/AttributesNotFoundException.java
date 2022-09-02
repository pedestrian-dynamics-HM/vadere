package org.vadere.state.attributes.exceptions;

import org.vadere.util.Attributes;

@SuppressWarnings("serial")
public class AttributesNotFoundException extends RuntimeException {
	public AttributesNotFoundException(Class<? extends Attributes> attributesClass) {
		super(attributesClass.getName());
	}
}
