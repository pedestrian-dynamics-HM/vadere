package org.vadere.state.traci;

import org.vadere.state.traci.TraCIException;

/**
 * Use this Exception if the message produces has no mean for a TraCI client. The stacktrace is
 * printed but the client gets a static response.
 */
public class TraCIExceptionInternal extends TraCIException {

	private final static String CLIENT_MESSAGE = "Internal error occurred in TraCI server. See server logs for details";

	public TraCIExceptionInternal(String message) {
		super(message);
	}

	public TraCIExceptionInternal(String message, Object... arg) {
		super(message, arg);
	}

	public TraCIExceptionInternal(String message, Throwable cause, Object... arg) {
		super(message, cause, arg);
	}

	public TraCIExceptionInternal(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public String getMessageForClient() {
		return CLIENT_MESSAGE;
	}
}
