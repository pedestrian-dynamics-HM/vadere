package org.vadere.state.traci;

import org.vadere.state.traci.TraCIException;

public class TraCICommandCreationException extends TraCIException {
	public TraCICommandCreationException(String message) {
		super(message);
	}

	public TraCICommandCreationException(String message, Object... arg) {
		super(message, arg);
	}

	public TraCICommandCreationException(String message, Throwable cause, Object... arg) {
		super(message, cause, arg);
	}

	public TraCICommandCreationException(String message, Throwable cause) {
		super(message, cause);
	}
}
