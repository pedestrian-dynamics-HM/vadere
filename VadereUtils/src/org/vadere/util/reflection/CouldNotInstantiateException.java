package org.vadere.util.reflection;

public class CouldNotInstantiateException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CouldNotInstantiateException(Throwable cause) {
		super(cause);
	}

	public CouldNotInstantiateException() {}

}
