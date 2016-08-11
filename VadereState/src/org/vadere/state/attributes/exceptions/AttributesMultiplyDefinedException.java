package org.vadere.state.attributes.exceptions;

import org.vadere.state.attributes.Attributes;

@SuppressWarnings("serial")
public class AttributesMultiplyDefinedException extends RuntimeException {
	public AttributesMultiplyDefinedException(Class<? extends Attributes> attributesClass) {
		super(attributesClass.getName());
	}
}
