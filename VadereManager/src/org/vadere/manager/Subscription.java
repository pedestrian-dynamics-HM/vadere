package org.vadere.manager;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commandHandler.CommandHandler;
import org.vadere.manager.traci.commandHandler.TraCICmdHandler;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.commands.TraCIValueSubscriptionCommand;
import org.vadere.manager.traci.response.StatusResponse;
import org.vadere.manager.traci.response.TraCIGetResponse;
import org.vadere.manager.traci.response.TraCIStatusResponse;
import org.vadere.manager.traci.response.TraCISubscriptionResponse;
import org.vadere.util.logging.Logger;

import java.util.Arrays;


/**
 * Wrapper around a given TraCIValueSubscriptionCommand to execute the
 * subscription
 * todo: merge multiple subscription so no unnecessary duplicates are send  send
 */
public class Subscription {

	private static Logger logger = Logger.getLogger(Subscription.class);

	private final TraCICmdHandler traCICmdHandler;
	private final TraCICmd responseIdentifier;
	private final TraCIValueSubscriptionCommand valueSubscriptionCommand;
	private boolean markedForRemoval;

	public Subscription(TraCICmdHandler traCICmdHandler, TraCICmd responseIdentifier, TraCIValueSubscriptionCommand valueSubscriptionCommand) {
		this.traCICmdHandler = traCICmdHandler;
		this.responseIdentifier = responseIdentifier;
		this.valueSubscriptionCommand = valueSubscriptionCommand;
		this.markedForRemoval = false;
	}

	public void executeSubscription(RemoteManager remoteManager) {

		// todo check if subscription is still valid.
//		markForRemoval();
//		if (markedForRemoval){
//			return;
//		}

		TraCISubscriptionResponse subResponse = new TraCISubscriptionResponse(
				new StatusResponse(valueSubscriptionCommand.getTraCICmd(), TraCIStatusResponse.OK, ""),
				responseIdentifier, valueSubscriptionCommand.getElementIdentifier(), valueSubscriptionCommand.getNumberOfVariables());

		for (TraCIGetCommand getCmd : valueSubscriptionCommand.getGetCommands()) {
			traCICmdHandler.handel(getCmd, remoteManager);
			TraCIGetResponse getResponse = getCmd.getResponse();

			if (getResponse.getStatusResponse().getResponse().equals(TraCIStatusResponse.ERR)) {
				logger.warn("Get command returned error: " + getResponse.getStatusResponse().getDescription());
				if (getResponse.getStatusResponse().getDescription().equals(CommandHandler.ELEMENT_ID_NOT_FOUND)) {
					logger.warn("Mark Subscription for removal. Subscribed element no longer exists");
					markForRemoval();
					logger.warnf(toString());
					break;
				}
			}

			subResponse.addVariableResponse(getResponse.getVariableIdentifier(),
					getResponse.getStatusResponse().getResponse(),
					getResponse.getResponseDataType(),
					getResponse.getResponseData());
		}

		if (markedForRemoval) {
			valueSubscriptionCommand.setResponse(
					TraCISubscriptionResponse.removeResponse(valueSubscriptionCommand, responseIdentifier));
		} else {
			valueSubscriptionCommand.setResponse(subResponse);
		}
	}

	public void markIfOld(double simTime){
		if (this.valueSubscriptionCommand.getEndTime() >= simTime){
			markForRemoval();
		}
	}

	public void markForRemoval() {
		this.markedForRemoval = true;
	}

	public boolean isMarkedForRemoval() {
		return markedForRemoval;
	}

	public TraCICmdHandler getTraCICmdHandler() {
		return traCICmdHandler;
	}

	public TraCICmd getResponseIdentifier() {
		return responseIdentifier;
	}

	public TraCIValueSubscriptionCommand getValueSubscriptionCommand() {
		return valueSubscriptionCommand;
	}

	public String getSubscriptionId() {
		return "SubID_" + valueSubscriptionCommand.getTraCICmd().name() + "-" + valueSubscriptionCommand.getElementIdentifier();
	}

	@Override
	public String toString() {
		String varList = Arrays.toString(valueSubscriptionCommand.getVariables().toArray());
		return "Subscription{ API=" + valueSubscriptionCommand.getTraCICmd().name() +
				" objectId='" + valueSubscriptionCommand.getElementIdentifier() + "' " +
				"subscribedVariables=" + varList + "}";
	}
}
