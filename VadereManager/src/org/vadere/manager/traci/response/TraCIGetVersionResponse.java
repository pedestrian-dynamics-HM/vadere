package org.vadere.manager.traci.response;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.TraCIVersion;
import org.vadere.manager.traci.reader.TraCICommandBuffer;

import java.util.Objects;

/**
 * Response object for {@link org.vadere.manager.traci.commands.control.TraCIGetVersionCommand}
 * command. It returns the numerical {@link #versionId} and string {@link #versionString}
 * representation of the current TraCI version.
 *
 * See {@link TraCIResponse} for static factory methods used to create objects from byte[]
 */
public class TraCIGetVersionResponse extends TraCIResponse {

	private int versionId;
	private String versionString;

	// deserialize from buffer (wrap byte[])
	public TraCIGetVersionResponse(StatusResponse statusResponse, TraCICommandBuffer buffer) {
		super(statusResponse, TraCICmd.GET_VERSION);
		this.versionId = buffer.readInt();
		this.versionString = buffer.readString();
	}

	public TraCIGetVersionResponse() {
		super(new StatusResponse(TraCICmd.GET_VERSION, TraCIStatusResponse.OK, ""),
				TraCICmd.GET_VERSION);
	}

	public TraCIGetVersionResponse(TraCIVersion version) {
		this();
		this.versionId = version.traciBaseVersion;
		this.versionString = version.getVersionString();
	}

	public boolean isOKResponseStatus() {
		return statusResponse.getResponse().equals(TraCIStatusResponse.OK);
	}

	public int getVersionId() {
		return versionId;
	}

	public void setVersionId(int versionId) {
		this.versionId = versionId;
	}

	public String getVersionString() {
		return versionString;
	}

	public void setVersionString(String versionString) {
		this.versionString = versionString;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TraCIGetVersionResponse that = (TraCIGetVersionResponse) o;
		return versionId == that.versionId &&
				Objects.equals(versionString, that.versionString);
	}

	@Override
	public int hashCode() {
		return Objects.hash(versionId, versionString);
	}

	@Override
	public String toString() {
		return "TraCIGetVersionResponse{" +
				"versionId=" + versionId +
				", versionString='" + versionString + '\'' +
				", statusResponse=" + statusResponse +
				", responseIdentifier=" + responseIdentifier +
				'}';
	}
}
