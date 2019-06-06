package org.vadere.manager.stsc.respons;

import org.vadere.manager.TraCIException;
import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.reader.TraCICommandBuffer;

import java.nio.ByteBuffer;
import java.util.Objects;

public class StatusResponse {

	private TraCICmd cmdIdentifier;
	private TraCIStatusResponse response;
	private String description;

	public static StatusResponse createFromByteBuffer(ByteBuffer rawCmd){
		StatusResponse ret = new StatusResponse();
		TraCICommandBuffer buf = TraCICommandBuffer.wrap(rawCmd);
		int cmdIdentifierCode = buf.readCmdIdentifier();
		ret.cmdIdentifier = TraCICmd.fromId(cmdIdentifierCode);
		if (ret.cmdIdentifier.equals(TraCICmd.UNKNOWN_CMD))
			throw new TraCIException("Unknown command in status response: " + cmdIdentifierCode);
		int status = buf.reader.readUnsignedByte();
		ret.response = TraCIStatusResponse.fromId(status);
		if (ret.response.equals(TraCIStatusResponse.UNKNOWN))
			throw new TraCIException("Unknown status response: " + status);

		ret.description = buf.reader.readString();

		return ret;
	}

	private StatusResponse(){

	}

	public StatusResponse(TraCICmd cmdIdentifier, TraCIStatusResponse response, String description) {
		this.cmdIdentifier = cmdIdentifier;
		this.response = response;
		this.description = description;
	}


	public TraCICmd getCmdIdentifier() {
		return cmdIdentifier;
	}

	public void setCmdIdentifier(TraCICmd cmdIdentifier) {
		this.cmdIdentifier = cmdIdentifier;
	}

	public TraCIStatusResponse getResponse() {
		return response;
	}

	public void setResponse(TraCIStatusResponse response) {
		this.response = response;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StatusResponse response1 = (StatusResponse) o;
		return cmdIdentifier == response1.cmdIdentifier &&
				response == response1.response &&
				description.equals(response1.description);
	}

	@Override
	public int hashCode() {
		return Objects.hash(cmdIdentifier, response, description);
	}

	@Override
	public String toString() {
		return "StatusResponse{" +
				"cmdIdentifier=" + cmdIdentifier +
				", response=" + response +
				", description='" + description + '\'' +
				'}';
	}
}
