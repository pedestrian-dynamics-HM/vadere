package org.vadere.manager.traci.commandHandler;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.manager.RemoteManager;
import org.vadere.manager.Subscription;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.TraCIDataType;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.commands.TraCIValueSubscriptionCommand;
import org.vadere.manager.traci.respons.StatusResponse;
import org.vadere.manager.traci.respons.TraCIGetResponse;
import org.vadere.manager.traci.respons.TraCIStatusResponse;
import org.vadere.manager.traci.writer.TraCIPacket;
import org.vadere.util.logging.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * {@link CommandHandler} classes perform the actual request within the command.
 *
 * See {@link CommandExecutor} on how commands are dispatched to the correct {@link CommandHandler}
 * subclass. These classes implement methods which adhere to the TraCICmdHandler Interface. These
 * methods are used by the {@link CommandExecutor} for dispatching.
 *
 */
public abstract class CommandHandler <VAR extends Enum> {

	private static Logger logger = Logger.getLogger(CommandHandler.class);

	public static final String ELEMENT_ID_NOT_FOUND = "No element found with given object id";
	protected HashMap<Pair<TraCICmd, VAR>, Method> handler;
	private final Method processNotImplemented;

	public CommandHandler() {
		handler = new HashMap<>();
		Method tmp = null;
		for (Method m : CommandHandler.class.getMethods()){
			if (m.getName().equals("process_NotImplemented")){
				tmp = m;
				break;
			}
		}
		assert (tmp != null) : "No Method found with name 'process_NotImplemented'";
		processNotImplemented = tmp;
	}

	protected Method getHandler(TraCICmd cmd, VAR var){
		return handler.getOrDefault(Pair.of(cmd, var), processNotImplemented);
	}

	protected abstract void init_HandlerSingle(Method m);
	protected abstract void init_HandlerMult(Method m);

	protected void putHandler(TraCICmd cmd, VAR var, Method m){
		logger.debugf("Pair: %s | %s Method: %s", cmd.name(), var.name(), m.getName());
		handler.put(Pair.of(cmd, var), m);
	}

	protected void init(Class<? extends Annotation> singleAnnotation, Class<? extends Annotation> multAnnotation){
		for (Method m : this.getClass().getDeclaredMethods()){
			logger.infof(m.getName());
			if (m.isAnnotationPresent(singleAnnotation)){
				init_HandlerSingle(m);
			}
			if (m.isAnnotationPresent(multAnnotation)){
				init_HandlerMult(m);
			}
		}
	}

	protected TraCICommand invokeHandler(Method m, Object obj, TraCICommand cmd, RemoteManager manager){
		try {
			return (TraCIGetCommand) m.invoke(obj, cmd, manager);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return process_UnknownCommand(cmd, manager);
	}


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
										TraCICmdHandler traCICmdHandler, TraCICmd getCommand, TraCICmd apiCmdResponse){
		TraCIValueSubscriptionCommand cmd = (TraCIValueSubscriptionCommand)rawCmd;

		List<TraCIGetCommand> getCommands = new ArrayList<>();

		cmd.getVariables().forEach(var -> {
			getCommands.add(new TraCIGetCommand(getCommand, var, cmd.getElementIdentifier()));
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
