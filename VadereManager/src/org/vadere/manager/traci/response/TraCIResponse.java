package org.vadere.manager.traci.response;

import org.vadere.state.traci.TraCIException;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.reader.TraCICommandBuffer;

import java.nio.ByteBuffer;


/**
 * Generic response object for each command. It contains the {@link StatusResponse} object as well
 * as optional response data warped in a second command.
 *
 * If no additional data is send the responseIdentifier will be ignored.
 *
 * Construction Methods: (compare with {@link org.vadere.manager.traci.commands.TraCICommand})
 *
 * Each {@link TraCIResponse} class is build in two ways. Either it is created from bytes (i.e. some
 * byte[] wrapped in a {@link ByteBuffer} or {@link TraCICommandBuffer} for ease of use) or manually
 * to prepaid a response to a client.
 */
public class TraCIResponse {

	protected StatusResponse statusResponse;
	protected TraCICmd responseIdentifier;


	public TraCIResponse(StatusResponse statusResponse, TraCICmd responseIdentifier) {
		this.statusResponse = statusResponse;
		this.responseIdentifier = responseIdentifier;
	}

	public TraCIResponse(TraCICmd responseIdentifier) {

	}

	// factory methods

	public static TraCIResponse create(StatusResponse statusResponse) {
		return new TraCIResponse(statusResponse, statusResponse.getCmdIdentifier());
	}

	public static TraCIResponse create(StatusResponse statusResponse, ByteBuffer rawCmd) {
		TraCICommandBuffer cmdResponseBuffer = TraCICommandBuffer.wrap(rawCmd);

		TraCICmd commandIdentifier = statusResponse.getCmdIdentifier();

		// build correct versions. based on actual command
		TraCICmd responseIdentifier;
		switch (commandIdentifier.type) {
			case CTRL:
				return createControlResponse(commandIdentifier, cmdResponseBuffer, statusResponse);
			case VALUE_GET:
				responseIdentifier = TraCICmd.fromId(cmdResponseBuffer.readCmdIdentifier());
				return new TraCIGetResponse(statusResponse, responseIdentifier, cmdResponseBuffer);
			case VALUE_SET:
				responseIdentifier = TraCICmd.fromId(cmdResponseBuffer.readCmdIdentifier());
				return createSetResponse(commandIdentifier, responseIdentifier, cmdResponseBuffer, statusResponse);
			case VALUE_SUB:
//				return createControlResponse(cmd, responseIdentifier, cmdResponseBuffer);
			case CONTEXT_SUB:
//				return createControlResponse(cmd, responseIdentifier, cmdResponseBuffer);
			default:
				throw new TraCIException("Response Object not implemented for command: " + commandIdentifier.toString());
		}
	}

	// instance methods

	private static TraCIResponse createControlResponse(TraCICmd commandIdentifier, TraCICommandBuffer cmdResponseBuffer, StatusResponse statusResponse) {
		switch (commandIdentifier) {
			case GET_VERSION:
				return new TraCIGetVersionResponse(statusResponse, cmdResponseBuffer);
			case SIM_STEP:
				return new TraCISimTimeResponse(statusResponse, cmdResponseBuffer);
		}
		return null;
	}

	private static TraCIResponse createSetResponse(TraCICmd commandIdentifier, TraCICmd responseIdentifier, TraCICommandBuffer cmdResponseBuffer, StatusResponse statusResponse) {
		return null;
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

	public boolean isErr() {
		return this.statusResponse.getResponse().equals(TraCIStatusResponse.ERR);
	}

	public boolean isOK() {
		return this.statusResponse.getResponse().equals(TraCIStatusResponse.OK);
	}

	public boolean isNotImpl() {
		return this.statusResponse.getResponse().equals(TraCIStatusResponse.NOT_IMPLEMENTED);
	}


	@Override
	public String toString() {
		return "TraCIResponse{" +
				"statusResponse=" + statusResponse +
				", responseIdentifier=" + responseIdentifier +
				'}';
	}
}
