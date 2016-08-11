package org.vadere.util.reflection;

/**
 * Unchecked wrapper around checked {@link java.lang.ClassNotFoundException}. To be used with
 * {@link org.vadere.util.reflection.DynamicClassInstantiator}.
 */
public class VadereClassNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public VadereClassNotFoundException(ClassNotFoundException cause) {
		super(cause);
	}

	public VadereClassNotFoundException() {

	}

}
