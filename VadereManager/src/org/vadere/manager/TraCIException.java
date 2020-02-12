package org.vadere.manager;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commands.TraCIGetCommand;

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

	public static TraCIException cmdErr(TraCICmd cmd, Throwable cause) {
		return new TraCIException("Error creating command: " + cmd.toString(), cause);
	}

	public static TraCIException cmdErrDatatype(TraCICmd cmd, Throwable cause) {
		return new TraCIException("Error creating Datatype: " + cmd.toString(), cause);
	}

	public static TraCIException cmdErrVariableType(TraCICmd cmd, Throwable cause) {
		return new TraCIException("Error creating PersonVar: " + cmd.toString(), cause);
	}

	public static TraCIException getNotImplemented(TraCIGetCommand cmd) {
		return new TraCIException("GetCommand for variableIdentifier " + cmd.getVariableIdentifier()
				+ "not supported in API: " + cmd.getTraCICmd().toString());
	}

	public String getMessageForClient() {
		return getMessage();
	}

}
