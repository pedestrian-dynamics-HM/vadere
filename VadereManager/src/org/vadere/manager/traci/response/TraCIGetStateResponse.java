package org.vadere.manager.traci.response;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.reader.TraCICommandBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Response object for {@link org.vadere.manager.traci.commands.control.TraCIGetStateCommand}
 * command. It includes all subscriptions previously added by each client.
 *
 * See {@link TraCIResponse} for static factory methods used to create objects from byte[]
 */
public class TraCIGetStateResponse extends TraCIResponse {

	//	private int numberOfSubscriptions;
	private List<TraCISubscriptionResponse> subscriptionResponses;


	public TraCIGetStateResponse(StatusResponse statusResponse, TraCICommandBuffer buffer) {
		this(statusResponse);
		int numberOfSubscriptions = buffer.readInt();
		for (int i = 0; i < numberOfSubscriptions; i++) {
			int zeroByte = buffer.readUnsignedByte();
			int length = buffer.readInt();
			TraCICmd responseIdentifier = TraCICmd.fromId(buffer.readUnsignedByte());
			subscriptionResponses.add(new TraCISubscriptionResponse(statusResponse, responseIdentifier, buffer));
		}
	}

	public TraCIGetStateResponse(StatusResponse statusResponse) {
		super(statusResponse, TraCICmd.GET_STATE);
		subscriptionResponses = new ArrayList<>();
	}

	public int getNumberOfSubscriptions() {
		return subscriptionResponses.size();
	}

	public List<TraCISubscriptionResponse> getSubscriptionResponses() {
		return subscriptionResponses;
	}

	public void setSubscriptionResponses(List<TraCISubscriptionResponse> subscriptionResponses) {
		this.subscriptionResponses = subscriptionResponses;
	}

	public void addSubscriptionResponse(TraCISubscriptionResponse response) {
		this.subscriptionResponses.add(response);
	}


}
