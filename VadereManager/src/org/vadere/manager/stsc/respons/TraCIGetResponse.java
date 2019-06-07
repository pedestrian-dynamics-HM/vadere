package org.vadere.manager.stsc.respons;

import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.TraCIDataType;
import org.vadere.manager.stsc.reader.TraCICommandBuffer;

import java.util.Objects;

public class TraCIGetResponse extends TraCIResponse{

	private int variableIdentifier;
	private String elementIdentifier;

	private TraCIDataType responseDataType;
	private Object responseData;

	public TraCIGetResponse(StatusResponse statusResponse, TraCICmd responseIdentifier, TraCICommandBuffer buffer) {
		super(statusResponse, responseIdentifier);

		variableIdentifier = buffer.reader.readUnsignedByte();
		elementIdentifier = buffer.reader.readString();
		responseDataType = TraCIDataType.fromId(buffer.reader.readUnsignedByte());
		responseData = buffer.reader.readTypeValue(responseDataType);
	}

	public TraCIGetResponse(StatusResponse statusResponse, TraCICmd responseIdentifier) {
		super(statusResponse, responseIdentifier);
	}

	public int getVariableIdentifier() {
		return variableIdentifier;
	}

	public void setVariableIdentifier(int variableIdentifier) {
		this.variableIdentifier = variableIdentifier;
	}

	public String getElementIdentifier() {
		return elementIdentifier;
	}

	public void setElementIdentifier(String elementIdentifier) {
		this.elementIdentifier = elementIdentifier;
	}

	public TraCIDataType getResponseDataType() {
		return responseDataType;
	}

	public void setResponseDataType(TraCIDataType responseDataType) {
		this.responseDataType = responseDataType;
	}

	public Object getResponseData() {
		return responseData;
	}

	public void setResponseData(Object responseData) {
		this.responseData = responseData;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TraCIGetResponse that = (TraCIGetResponse) o;
		return responseDataType == that.responseDataType &&
				responseData.equals(that.responseData);
	}

	@Override
	public int hashCode() {
		return Objects.hash(responseDataType, responseData);
	}

	@Override
	public String toString() {
		return "TraCIGetResponse{" +
				"responseDataType=" + responseDataType +
				", responseData=" + responseData +
				'}';
	}
}
