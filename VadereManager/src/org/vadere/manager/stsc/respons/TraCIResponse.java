package org.vadere.manager.stsc.respons;

import org.vadere.manager.TraCIException;
import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.reader.TraCICommandBuffer;

import java.nio.ByteBuffer;

public class TraCIResponse {

	protected StatusResponse statusResponse;
	protected TraCICmd responseIdentifier;


	public static TraCIResponse create(StatusResponse statusResponse){
		return new TraCIResponse(statusResponse, statusResponse.getCmdIdentifier());
	}

	public static TraCIResponse create(StatusResponse statusResponse, ByteBuffer rawCmd){
		TraCICommandBuffer cmdResponseBuffer = TraCICommandBuffer.wrap(rawCmd);

		TraCICmd commandIdentifier = statusResponse.getCmdIdentifier();

		int identifier = cmdResponseBuffer.readCmdIdentifier();
		TraCICmd responseIdentifier = TraCICmd.fromId(identifier);
		if (responseIdentifier.equals(TraCICmd.UNKNOWN_CMD))
			throw new TraCIException("Unknown response identifier: " + identifier +
					" for command: " + commandIdentifier.id);

		// build correct versions. based on actual command
		switch (commandIdentifier.type){
			case CTRL:
				return createControlResponse(commandIdentifier, responseIdentifier, cmdResponseBuffer, statusResponse);
			case VALUE_GET:
				return createGetResponse(commandIdentifier, responseIdentifier, cmdResponseBuffer, statusResponse);
			case VALUE_SET:
				return createSetResponse(commandIdentifier, responseIdentifier, cmdResponseBuffer, statusResponse);
			case VALUE_SUB:
//				return createControlResponse(commandIdentifier, responseIdentifier, cmdResponseBuffer);
			case CONTEXT_SUB:
//				return createControlResponse(commandIdentifier, responseIdentifier, cmdResponseBuffer);
			default:
				throw new TraCIException("Response Object not implemented for command: " + commandIdentifier.toString());
		}
	}

	// factory methods

	private static TraCIResponse createControlResponse(TraCICmd commandIdentifier, TraCICmd responseIdentifier, TraCICommandBuffer cmdResponseBuffer, StatusResponse statusResponse){
		switch (commandIdentifier){
			case GET_VERSION:
				return new TraCIGetVersionResponse(statusResponse, cmdResponseBuffer);
			case SIM_STEP:
				return new TraCISimTimeResponse(statusResponse, cmdResponseBuffer);

		}
		return null;
	}

	private static TraCIResponse createGetResponse(TraCICmd commandIdentifier, TraCICmd responseIdentifier, TraCICommandBuffer cmdResponseBuffer, StatusResponse statusResponse){

		return null;
	}

	private static TraCIResponse createSetResponse(TraCICmd commandIdentifier, TraCICmd responseIdentifier, TraCICommandBuffer cmdResponseBuffer, StatusResponse statusResponse){

		return null;
	}


	// instance methods

	protected TraCIResponse (StatusResponse statusResponse, TraCICmd responseIdentifier
	){
		this.statusResponse = statusResponse;
		this.responseIdentifier = responseIdentifier;
	}


	public StatusResponse getStatusResponse() {
		return statusResponse;
	}

	public void setStatusResponse(StatusResponse statusResponse) {
		this.statusResponse = statusResponse;
	}

	public TraCICmd getResponseIdentifier() {
		return responseIdentifier;
	}

	public void setResponseIdentifier(TraCICmd responseIdentifier) {
		this.responseIdentifier = responseIdentifier;
	}

	@Override
	public String toString() {
		return "TraCIResponse{" +
				"statusResponse=" + statusResponse +
				", responseIdentifier=" + responseIdentifier +
				'}';
	}
}
