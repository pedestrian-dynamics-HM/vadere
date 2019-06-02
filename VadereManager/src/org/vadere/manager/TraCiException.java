package org.vadere.manager;

public class TraCiException extends RuntimeException {

	public TraCiException(String message) {
		super(message);
	}

	public TraCiException(String message, Throwable cause) {
		super(message, cause);
	}
}
