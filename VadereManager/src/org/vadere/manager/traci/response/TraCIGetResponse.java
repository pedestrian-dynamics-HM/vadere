package org.vadere.manager.traci.response;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.manager.traci.reader.TraCICommandBuffer;

import java.util.Objects;


/**
 * Response object for {@link org.vadere.manager.traci.commands.TraCIGetCommand} command. This
 * command is used to retrieve generic data from the simulator.
 *
 * The structure of the response:
 *
 * [ responseID(based on API) ] [ variableId ] [ elementId ] [ dataTypeId ] [ data ]
 *
 * - responseID(based on API): Each API (Person, Vehicle, Simulation, ...) has a different Id. -
 * variableId: Id for the var. The numbers may be the same between different APIs - elementId:
 * String based id for the object (i.e. a pedestrianId) - dataTypeId: see {@link TraCIDataType} -
 * data: data to be returned.
 *
 * See {@link TraCIResponse} for static factory methods used to create objects from byte[]
 */
public class TraCIGetResponse extends TraCIResponse {

	private int variableIdentifier;
	private String elementIdentifier;

	private TraCIDataType responseDataType;
	private Object responseData;

	public TraCIGetResponse(StatusResponse statusResponse, TraCICmd responseIdentifier, TraCICommandBuffer buffer) {
		super(statusResponse, responseIdentifier);

		variableIdentifier = buffer.readUnsignedByte();
		elementIdentifier = buffer.readString();
		responseDataType = TraCIDataType.fromId(buffer.readUnsignedByte());
		responseData = buffer.readTypeValue(responseDataType);
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
