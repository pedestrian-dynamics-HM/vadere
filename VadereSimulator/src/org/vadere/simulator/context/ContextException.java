package org.vadere.simulator.context;

public class ContextException extends RuntimeException {
	public ContextException(String message) {
		super(message);
	}

	public ContextException(String message, Throwable cause) {
		super(message, cause);
	}

	public ContextException(Throwable cause) {
		super(cause);
	}

	protected ContextException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
