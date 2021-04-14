package org.vadere.manager.traci.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.manager.traci.commandHandler.annotation.VehicleHandler;
import org.vadere.manager.traci.commandHandler.annotation.VehicleHandlers;
import org.vadere.manager.traci.commandHandler.variables.VehicleVar;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.response.TraCIGetResponse;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class VehicleCommandHandler extends CommandHandler<VehicleVar> {

	public static VehicleCommandHandler instance;

//	private HashMap<PersonVar, Method> handler;

	static {
		instance = new VehicleCommandHandler();
	}

	public VehicleCommandHandler() {
		super();
		init(VehicleHandler.class, VehicleHandlers.class);
	}

	@Override
	protected void init_HandlerSingle(Method m) {
		VehicleHandler an = m.getAnnotation(VehicleHandler.class);
		putHandler(an.cmd(), an.var(), m);
	}

	@Override
	protected void init_HandlerMult(Method m) {
		VehicleHandler[] ans = m.getAnnotation(VehicleHandlers.class).value();
		for (VehicleHandler a : ans) {
			putHandler(a.cmd(), a.var(), m);
		}
	}

	public TraCIGetResponse responseOK(TraCIDataType responseDataType, Object responseData) {
		return responseOK(responseDataType, responseData, TraCICmd.GET_VEHICLE_VALUE, TraCICmd.RESPONSE_GET_VEHICLE_VALUE);
	}

	public TraCIGetResponse responseERR(String err) {
		return responseERR(err, TraCICmd.GET_VEHICLE_VALUE, TraCICmd.RESPONSE_GET_VEHICLE_VALUE);
	}

	public TraCICommand process_getIDList(TraCIGetCommand rawCmd, RemoteManager remoteManager, VehicleVar traCIVar) {

		// always return an empty list
		rawCmd.setResponse(responseOK(TraCIDataType.STRING_LIST, new ArrayList<>()));

		return rawCmd;
	}


	public TraCICommand processValueSub(TraCICommand rawCmd, RemoteManager remoteManager) {
		return processValueSub(rawCmd, remoteManager, this::processGet,
				TraCICmd.GET_VEHICLE_VALUE, TraCICmd.RESPONSE_SUB_VEHICLE_VALUE);
	}

	public TraCICommand processGet(TraCICommand cmd, RemoteManager remoteManager) {
		TraCIGetCommand getCmd = (TraCIGetCommand) cmd;

		VehicleVar var = VehicleVar.fromId(getCmd.getVariableIdentifier());

		switch (var) {
			case ID_LIST:
				return process_getIDList(getCmd, remoteManager, var);
			default:
				return process_UnknownCommand(getCmd, remoteManager);
		}
	}

}
