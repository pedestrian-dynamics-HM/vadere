package org.vadere.state.traci;

public class TraCIException extends RuntimeException {

	public TraCIException(String message) {
		super(message);
	}

	public TraCIException(String message, Object... arg) {
		super(String.format(message, arg));
	}

	public TraCIException(String message, Throwable cause, Object... arg) {
		super(String.format(message, arg), cause);
	}

	public TraCIException(String message, Throwable cause) {
		super(message, cause);
	}

	public static TraCIException cmdErr(String cmd, Throwable cause) {
		return new TraCIException("Error creating command: " + cmd.toString(), cause);
	}

	public static TraCIException cmdErrDatatype(String cmd, Throwable cause) {
		return new TraCIException("Error creating Datatype: " + cmd.toString(), cause);
	}

	public static TraCIException cmdErrVariableType(String cmd, Throwable cause) {
		return new TraCIException("Error creating PersonVar: " + cmd.toString(), cause);
	}

	public static TraCIException getNotImplemented(String varId, String cmdId) {
		return new TraCIException("GetCommand for variableIdentifier " + varId
				+ "not supported in API: " + cmdId);
	}

	public String getMessageForClient() {
		return getMessage();
	}

}
