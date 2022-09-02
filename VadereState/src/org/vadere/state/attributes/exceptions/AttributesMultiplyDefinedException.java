package org.vadere.state.attributes.exceptions;

import org.vadere.util.Attributes;

@SuppressWarnings("serial")
public class AttributesMultiplyDefinedException extends RuntimeException {
	public AttributesMultiplyDefinedException(Class<? extends Attributes> attributesClass) {
		super(attributesClass.getName());
	}
}
