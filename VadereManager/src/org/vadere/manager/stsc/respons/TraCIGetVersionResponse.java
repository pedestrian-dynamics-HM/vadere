package org.vadere.manager.stsc.respons;

import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.reader.TraCICommandBuffer;

import java.util.Objects;

public class TraCIGetVersionResponse extends TraCIResponse{

	private int versionId;
	private String versionString;

	public TraCIGetVersionResponse(StatusResponse statusResponse, TraCICommandBuffer buffer) {
		super(statusResponse, TraCICmd.GET_VERSION);
		this.versionId = buffer.reader.readInt();
		this.versionString = buffer.reader.readString();
	}

	public TraCIGetVersionResponse(){
		super(new StatusResponse(TraCICmd.GET_VERSION, TraCIStatusResponse.OK, ""),
				TraCICmd.GET_VERSION);
	}

	public TraCIGetVersionResponse(int versionId, String versionString) {
		this();
		this.versionId = versionId;
		this.versionString = versionString;
	}

	public boolean isOKResponseStatus(){
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