package org.vadere.manager.traci.commandHandler;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.manager.RemoteManager;
import org.vadere.manager.Subscription;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIValueSubscriptionCommand;
import org.vadere.manager.traci.response.StatusResponse;
import org.vadere.manager.traci.response.TraCIGetResponse;
import org.vadere.manager.traci.response.TraCIStatusResponse;
import org.vadere.manager.traci.writer.TraCIPacket;
import org.vadere.util.logging.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;


/**
 * {@link CommandHandler} classes perform the actual request within the command.
 *
 * See {@link CommandExecutor} on how commands are dispatched to the correct {@link CommandHandler}
 * subclass. These classes implement methods which adhere to the TraCICmdHandler Interface. These
 * methods are used by the {@link CommandExecutor} for dispatching.
 */
public abstract class CommandHandler<VAR extends Enum> {

	public static final String ELEMENT_ID_NOT_FOUND = "No element found with given object id ";
	public static final String ELEMENT_ID_NOT_FREE = "There is already an element with the given object id ";
	public static final String FILE_NOT_FOUND = "No file with given path ";
	public static final String COULD_NOT_PARSE_OBJECT_FROM_JSON = "Could not parse object from given json ";
	public static final String COULD_NOT_MAP_OBJECT_FROM_JSON = "Could not map object from given json ";
	public static final String COULD_NOT_SERIALIZE_OBJECT = "Could not serialize object ";
	public static final String NO_MAIN_MODEL = "Main Model is not present.";
	private static Logger logger = Logger.getLogger(CommandHandler.class);
	private final Method processNotImplemented;
	protected HashMap<Pair<TraCICmd, VAR>, Method> handler;

	public CommandHandler() {
		handler = new HashMap<>();
		Method tmp = null;
		for (Method m : CommandHandler.class.getMethods()) {
			if (m.getName().equals("process_NotImplemented")) {
				tmp = m;
				break;
			}
		}
		assert (tmp != null) : "No Method found with name 'process_NotImplemented'";
		processNotImplemented = tmp;
	}

	protected Method getHandler(TraCICmd cmd, VAR var) {
		return handler.getOrDefault(Pair.of(cmd, var), processNotImplemented);
	}

	protected abstract void init_HandlerSingle(Method m);

	protected abstract void init_HandlerMult(Method m);

	protected void putHandler(TraCICmd cmd, VAR var, Method m) {
		logger.debugf("Pair: %s | %s Method: %s", cmd.name(), var.name(), m.getName());
		handler.put(Pair.of(cmd, var), m);
	}

	protected void init(Class<? extends Annotation> singleAnnotation, Class<? extends Annotation> multAnnotation) {
		for (Method m : this.getClass().getDeclaredMethods()) {
			logger.tracef(m.getName());
			if (m.isAnnotationPresent(singleAnnotation)) {
				init_HandlerSingle(m);
			}
			if (m.isAnnotationPresent(multAnnotation)) {
				init_HandlerMult(m);
			}
		}
	}

	protected TraCICommand invokeHandler(Method m, Object obj, TraCICommand cmd, RemoteManager manager) {
		try {
			return (TraCICommand) m.invoke(obj, cmd, manager);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return process_UnknownCommand(cmd, manager);
	}


	public TraCICommand process_NotImplemented(TraCICommand cmd, RemoteManager remoteManager) {
		return cmd.setNOK_response(TraCIPacket.sendStatus(cmd.getTraCICmd(),
				TraCIStatusResponse.NOT_IMPLEMENTED,
				"Command " + cmd.getCmdType().toString() + "not Implemented"));
	}

	public TraCICommand process_UnknownCommand(TraCICommand cmd, RemoteManager remoteManager) {
		return cmd.setNOK_response(TraCIPacket.sendStatus(cmd.getTraCICmd(),
				TraCIStatusResponse.ERR,
				"Command " + cmd.getCmdType().toString() + "Unknown"));
	}

	public TraCIGetResponse responseOK(TraCIDataType responseDataType, Object responseData, TraCICmd apiCmd, TraCICmd apiCmdResponse) {
		TraCIGetResponse res = new TraCIGetResponse(
				new StatusResponse(apiCmd, TraCIStatusResponse.OK, ""),
				apiCmdResponse);
		res.setResponseDataType(responseDataType);
		res.setResponseData(responseData);
		return res;
	}

	public TraCIGetResponse responseERR(String err, TraCICmd apiCmd, TraCICmd apiCmdResponse) {
		return new TraCIGetResponse(new StatusResponse(apiCmd, TraCIStatusResponse.ERR, err), apiCmdResponse);
	}


	public TraCICommand processValueSub(TraCICommand rawCmd,
										RemoteManager remoteManager,
										TraCICmdHandler traCICmdHandler,
										TraCICmd getCommand,
										TraCICmd apiCmdResponse) {
		TraCIValueSubscriptionCommand cmd = (TraCIValueSubscriptionCommand) rawCmd;

		cmd.buildGetCommands(getCommand);

		// add correct Get-Handler and the subscription command to the remoteManager
		// after each SIM_STEP command the remoteMangers knows how to gather
		// the subscribed variables. It is the responsibility of the
		// TraCIValueSubscriptionCommand implementation to translate the TraCIGetResponses
		// into a single TraCISubscriptionResponse.
		Subscription sub = new Subscription(traCICmdHandler, apiCmdResponse, cmd);
		remoteManager.addValueSubscription(sub);

		// process the current subscription to return the initial response for the given subscription
		// return value not needed. The result is directly saved in getCmd.setResponse(...)
		// in the process_X methods.
		sub.executeSubscription(remoteManager);

		return cmd;
	}
}
