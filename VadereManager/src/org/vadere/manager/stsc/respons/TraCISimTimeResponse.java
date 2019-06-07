package org.vadere.manager.stsc.respons;

import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.TraCIDataType;
import org.vadere.manager.stsc.reader.TraCICommandBuffer;

/**
 *  Response object for {@link org.vadere.manager.stsc.commands.control.TraCISimStepCommand}
 *  command. It includes all subscriptions previously added by each client.
 *
 *  See {@link TraCIResponse} for static factory methods used to create objects from byte[]
 */
public class TraCISimTimeResponse extends TraCIResponse {

	private Object subscriptionData;
	private TraCIDataType subscriptionDataType;


	public TraCISimTimeResponse (StatusResponse statusResponse, TraCICommandBuffer buffer){
		super(statusResponse, TraCICmd.SIM_STEP);
		subscriptionDataType = TraCIDataType.fromId(buffer.reader.readUnsignedByte());
		subscriptionData = buffer.reader.readTypeValue(subscriptionDataType);
	}

	public TraCISimTimeResponse( Object subscriptionData, TraCIDataType subscriptionDataType) {
		super(new StatusResponse(TraCICmd.SIM_STEP, TraCIStatusResponse.OK, ""),
				TraCICmd.SIM_STEP);
		this.subscriptionData = subscriptionData;
		this.subscriptionDataType = subscriptionDataType;
	}

	public Object getSubscriptionData() {
		return subscriptionData;
	}

	public void setSubscriptionData(Object subscriptionData) {
		this.subscriptionData = subscriptionData;
	}

	public TraCIDataType getSubscriptionDataType() {
		return subscriptionDataType;
	}

	public void setSubscriptionDataType(TraCIDataType subscriptionDataType) {
		this.subscriptionDataType = subscriptionDataType;
	}

	@Override
	public String toString() {
		return "TraCISimTimeResponse{" +
				"subscriptionData=" + subscriptionData +
				", subscriptionDataType=" + subscriptionDataType +
				'}';
	}
}
