package org.vadere.manager.traci.response;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.reader.TraCICommandBuffer;

import java.nio.ByteBuffer;
import java.util.Objects;


/**
 * {@link StatusResponse} object send with each response.
 *
 * see {@link TraCIStatusResponse} for status codes.
 */
public class StatusResponse {

	private TraCICmd cmdIdentifier;
	private TraCIStatusResponse response;
	private String description;

	private StatusResponse() {

	}

	public StatusResponse(TraCICmd cmdIdentifier, TraCIStatusResponse response, String description) {
		this.cmdIdentifier = cmdIdentifier;
		this.response = response;
		this.description = description;
	}

	public static StatusResponse createFromByteBuffer(ByteBuffer rawCmd) {
		StatusResponse ret = new StatusResponse();
		TraCICommandBuffer buf = TraCICommandBuffer.wrap(rawCmd);
		ret.cmdIdentifier = TraCICmd.fromId(buf.readCmdIdentifier());
		ret.response = TraCIStatusResponse.fromId(buf.readUnsignedByte());

		ret.description = buf.readString();

		return ret;
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

	boolean isErr() {
		return response.equals(TraCIStatusResponse.ERR);
	}

	boolean isOk() {
		return response.equals(TraCIStatusResponse.OK);
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
