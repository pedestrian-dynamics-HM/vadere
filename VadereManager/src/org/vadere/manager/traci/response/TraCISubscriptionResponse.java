package org.vadere.manager.traci.response;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.manager.traci.commands.TraCIValueSubscriptionCommand;
import org.vadere.manager.traci.reader.TraCICommandBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TraCISubscriptionResponse extends TraCIResponse {

	public static final String SUB_REMOVED = "Subscription removed.";

	private String elementId;
	private int numberOfVariables;
	private List<SingeVarResponse> responses;


	public TraCISubscriptionResponse(StatusResponse statusResponse, TraCICmd responseIdentifier, TraCICommandBuffer buffer) {
		this(statusResponse, responseIdentifier);

		elementId = buffer.readString();
		numberOfVariables = buffer.readUnsignedByte();
		for (int i = 0; i < numberOfVariables; i++) {
			responses.add(new SingeVarResponse(buffer));
		}
	}


	public TraCISubscriptionResponse(StatusResponse statusResponse, TraCICmd responseIdentifier, String elementId, int numberOfVariables) {
		this(statusResponse, responseIdentifier);
		this.elementId = elementId;
		this.numberOfVariables = numberOfVariables;
	}

	public TraCISubscriptionResponse(StatusResponse statusResponse, TraCICmd responseIdentifier) {
		super(statusResponse, responseIdentifier);
		responses = new ArrayList<>();
	}

	public static TraCISubscriptionResponse removeResponse(TraCIValueSubscriptionCommand cmd, TraCICmd res) {
		return new TraCISubscriptionResponse(
				new StatusResponse(cmd.getTraCICmd(), TraCIStatusResponse.ERR, SUB_REMOVED),
				res, cmd.getElementIdentifier(), cmd.getNumberOfVariables());
	}

	public void addVariableResponse(int variableId, TraCIStatusResponse status, TraCIDataType dataType, Object value) {
		responses.add(new SingeVarResponse(variableId, status, dataType, value));
	}

	public String getElementId() {
		return elementId;
	}

	public void setElementId(String elementId) {
		this.elementId = elementId;
	}

	public int getNumberOfVariables() {
		return numberOfVariables;
	}

	public void setNumberOfVariables(int numberOfVariables) {
		this.numberOfVariables = numberOfVariables;
	}

	public List<SingeVarResponse> getResponses() {
		return responses;
	}

	public void setResponses(List<SingeVarResponse> responses) {
		this.responses = responses;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TraCISubscriptionResponse that = (TraCISubscriptionResponse) o;
		return numberOfVariables == that.numberOfVariables &&
				elementId.equals(that.elementId) &&
				responses.equals(that.responses);
	}

	@Override
	public int hashCode() {
		return Objects.hash(elementId, numberOfVariables, responses);
	}

	@Override
	public String toString() {
		return "TraCISubscriptionResponse{" +
				"elementId='" + elementId + '\'' +
				", numberOfVariables=" + numberOfVariables +
				", responses=" + responses +
				", statusResponse=" + statusResponse +
				'}';
	}

	public class SingeVarResponse {

		int variableId;
		TraCIStatusResponse status;
		TraCIDataType variableDataType;
		Object variableValue;

		public SingeVarResponse(TraCICommandBuffer buffer) {
			variableId = buffer.readUnsignedByte();
			status = TraCIStatusResponse.fromId(buffer.readUnsignedByte());
			if (status.equals(TraCIStatusResponse.OK)) {
				variableDataType = TraCIDataType.fromId(buffer.readUnsignedByte());
			} else {
				variableDataType = TraCIDataType.STRING; // ERR
			}
			variableValue = buffer.readTypeValue(variableDataType);
		}

		public SingeVarResponse(int variableId, TraCIStatusResponse status, TraCIDataType variableDataType, Object variableValue) {
			this.variableId = variableId;
			this.status = status;
			this.variableDataType = variableDataType;
			this.variableValue = variableValue;
		}

		public boolean isStatusOK() {
			return status.equals(TraCIStatusResponse.OK);
		}

		public int getVariableId() {
			return variableId;
		}

		public void setVariableId(int variableId) {
			this.variableId = variableId;
		}

		public TraCIStatusResponse getStatus() {
			return status;
		}

		public void setStatus(TraCIStatusResponse status) {
			this.status = status;
		}

		public TraCIDataType getVariableDataType() {
			return variableDataType;
		}

		public void setVariableDataType(TraCIDataType variableDataType) {
			this.variableDataType = variableDataType;
		}

		public Object getVariableValue() {
			return variableValue;
		}

		public void setVariableValue(Object variableValue) {
			this.variableValue = variableValue;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SingeVarResponse that = (SingeVarResponse) o;
			return variableId == that.variableId &&
					status == that.status &&
					variableDataType == that.variableDataType &&
					variableValue.equals(that.variableValue);
		}

		@Override
		public int hashCode() {
			return Objects.hash(variableId, status, variableDataType, variableValue);
		}

		@Override
		public String toString() {
			return "SingeVarResponse{" +
					"variableId=" + variableId +
					", status=" + status +
					", variableDataType=" + variableDataType +
					", variableValue=" + variableValue +
					'}';
		}
	}

}
