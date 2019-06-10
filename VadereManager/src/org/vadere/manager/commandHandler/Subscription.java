package org.vadere.manager.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.commands.TraCIGetCommand;
import org.vadere.manager.stsc.commands.TraCIValueSubscriptionCommand;
import org.vadere.manager.stsc.respons.StatusResponse;
import org.vadere.manager.stsc.respons.TraCIGetResponse;
import org.vadere.manager.stsc.respons.TraCIStatusResponse;
import org.vadere.manager.stsc.respons.TraCISubscriptionResponse;

public class Subscription {

	TraCICmdHandler traCICmdHandler;
	TraCICmd responseIdentifier;
	TraCIValueSubscriptionCommand valueSubscriptionCommand;

	public Subscription(TraCICmdHandler traCICmdHandler, TraCICmd responseIdentifier, TraCIValueSubscriptionCommand valueSubscriptionCommand) {
		this.traCICmdHandler = traCICmdHandler;
		this.responseIdentifier = responseIdentifier;
		this.valueSubscriptionCommand = valueSubscriptionCommand;
	}

	public void executeSubscription(RemoteManager remoteManager){
		TraCISubscriptionResponse subResponse = new TraCISubscriptionResponse(
				new StatusResponse(valueSubscriptionCommand.getTraCICmd(), TraCIStatusResponse.OK, ""),
				responseIdentifier, valueSubscriptionCommand.getElementIdentifier(), valueSubscriptionCommand.getNumberOfVariables());

		for (TraCIGetCommand getCmd : valueSubscriptionCommand.getGetCommands()) {
			traCICmdHandler.handel(getCmd, remoteManager);
			TraCIGetResponse getResponse = getCmd.getResponse();
			subResponse.addVariableResponse(getResponse.getVariableIdentifier(),
					getResponse.getStatusResponse().getResponse(),
					getResponse.getResponseDataType(),
					getResponse.getResponseData());
		}

		valueSubscriptionCommand.setResponse(subResponse);

	}

	public TraCICmdHandler getTraCICmdHandler() {
		return traCICmdHandler;
	}

	public void setTraCICmdHandler(TraCICmdHandler traCICmdHandler) {
		this.traCICmdHandler = traCICmdHandler;
	}

	public TraCICmd getResponseIdentifier() {
		return responseIdentifier;
	}

	public void setResponseIdentifier(TraCICmd responseIdentifier) {
		this.responseIdentifier = responseIdentifier;
	}

	public TraCIValueSubscriptionCommand getValueSubscriptionCommand() {
		return valueSubscriptionCommand;
	}

	public void setValueSubscriptionCommand(TraCIValueSubscriptionCommand valueSubscriptionCommand) {
		this.valueSubscriptionCommand = valueSubscriptionCommand;
	}
}
