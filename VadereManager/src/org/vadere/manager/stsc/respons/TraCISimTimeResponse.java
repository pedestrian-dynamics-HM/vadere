package org.vadere.manager.stsc.respons;

import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.reader.TraCICommandBuffer;

public class TraCISimTimeResponse extends TraCIResponse {

	private Object subscriptionData;


	public TraCISimTimeResponse (StatusResponse statusResponse, TraCICommandBuffer buffer){
		super(statusResponse, TraCICmd.SIM_STEP);
		subscriptionData = buffer; // todo: read subscription response...
	}

	public TraCISimTimeResponse( Object subscriptionData) {
		super(new StatusResponse(TraCICmd.SIM_STEP, TraCIStatusResponse.OK, ""),
				TraCICmd.SIM_STEP);
		this.subscriptionData = subscriptionData;
	}

	public Object getSubscriptionData() {
		return subscriptionData;
	}

	public void setSubscriptionData(Object subscriptionData) {
		this.subscriptionData = subscriptionData;
	}
}
