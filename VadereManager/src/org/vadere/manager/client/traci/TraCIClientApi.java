package org.vadere.manager.client.traci;

import org.vadere.manager.TraCISocket;

public abstract class TraCIClientApi {

	protected TraCISocket socket;
	protected String apiName;

	public TraCIClientApi(TraCISocket socket, String apiName) {
		this.socket = socket;
		this.apiName = apiName;
	}
}
