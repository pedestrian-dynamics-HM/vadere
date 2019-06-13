package org.vadere.manager.traci.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.Subscription;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.TraCIDataType;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.commands.TraCIValueSubscriptionCommand;
import org.vadere.manager.traci.respons.StatusResponse;
import org.vadere.manager.traci.respons.TraCIGetResponse;
import org.vadere.manager.traci.writer.TraCIPacket;
import org.vadere.manager.traci.respons.TraCIStatusResponse;
import org.vadere.manager.traci.commands.TraCICommand;

import java.util.ArrayList;
import java.util.List;


/**
 * {@link CommandHandler} classes perform the actual request within the command.
 *
 * See {@link CommandExecutor} on how commands are dispatched to the correct {@link CommandHandler}
 * subclass. These classes implement methods which adhere to the TraCICmdHandler Interface. These
 * methods are used by the {@link CommandExecutor} for dispatching.
 *
 */
public abstract class CommandHandler {

	public static final String ELEMENT_ID_NOT_FOUND = "No element found with given object id";

	public TraCICommand process_NotImplemented(TraCICommand cmd, RemoteManager remoteManager){
		return cmd.setNOK_response(TraCIPacket.sendStatus(cmd.getTraCICmd(),
				TraCIStatusResponse.NOT_IMPLEMENTED,
				"Command " + cmd.getCmdType().toString() + "not Implemented"));
	}

	public TraCICommand process_UnknownCommand(TraCICommand cmd, RemoteManager remoteManager){
		return cmd.setNOK_response(TraCIPacket.sendStatus(cmd.getTraCICmd(),
				TraCIStatusResponse.ERR,
				"Command " + cmd.getCmdType().toString() + "Unknown"));
	}

	public TraCIGetResponse responseOK(TraCIDataType responseDataType, Object responseData, TraCICmd apiCmd, TraCICmd apiCmdResponse){
		TraCIGetResponse res = new TraCIGetResponse(
				new StatusResponse(apiCmd, TraCIStatusResponse.OK, ""),
				apiCmdResponse);
		res.setResponseDataType(responseDataType);
		res.setResponseData(responseData);
		return res;
	}

	public TraCIGetResponse responseERR(String err, TraCICmd apiCmd, TraCICmd apiCmdResponse){
		TraCIGetResponse res = new TraCIGetResponse(
				new StatusResponse(apiCmd, TraCIStatusResponse.ERR, err),
				apiCmdResponse);
		return res;
	}


	public TraCICommand processValueSub(TraCICommand rawCmd, RemoteManager remoteManager,
										TraCICmdHandler traCICmdHandler, TraCICmd apiCmd, TraCICmd apiCmdResponse){
		TraCIValueSubscriptionCommand cmd = (TraCIValueSubscriptionCommand)rawCmd;

		List<TraCIGetCommand> getCommands = new ArrayList<>();

		cmd.getVariables().forEach(var -> {
			getCommands.add(new TraCIGetCommand(apiCmd, var, cmd.getElementIdentifier()));
		});

		cmd.setGetCommands(getCommands);
		// add correct Get-Handler and the subscription command to the remoteManager
		// after each SIM_STEP command the remoteMangers knows how to gather
		// the subscribed variables. It is the responsibility of the
		// TraCIValueSubscriptionCommand implementation to translate the TraCIGetResponses
		// into a single TraCISubscriptionResponse.
		Subscription sub = new Subscription(traCICmdHandler, apiCmdResponse, cmd);
		remoteManager.addValueSubscription(sub );

		// process the current subscription to return the initial response for the given subscription
		// return value not needed. The result is directly saved in getCmd.setResponse(...)
		// in the process_X methods.
		sub.executeSubscription(remoteManager);

		return cmd;
	}
}
