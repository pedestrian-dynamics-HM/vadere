package org.vadere.manager.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.TraCIDataType;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.commands.TraCIGetCommand;
import org.vadere.manager.stsc.respons.TraCIGetResponse;

import java.util.ArrayList;

public class VehicleCommandHandler extends CommandHandler{

	public static VehicleCommandHandler instance;

//	private HashMap<TraCIPersonVar, Method> handler;

	static {
		instance = new VehicleCommandHandler();
	}

	public TraCIGetResponse responseOK(TraCIDataType responseDataType, Object responseData){
		return  responseOK(responseDataType, responseData, TraCICmd.GET_VEHICLE_VALUE, TraCICmd.RESPONSE_GET_VEHICLE_VALUE);
	}

	public TraCIGetResponse responseERR(String err){
		return responseERR(err, TraCICmd.GET_VEHICLE_VALUE, TraCICmd.RESPONSE_GET_VEHICLE_VALUE);
	}

	private TraCICommand process_getIDList(TraCIGetCommand rawCmd, RemoteManager remoteManager, TraCIVehicleVar traCIVar){

		// always return an empty list
		rawCmd.setResponse(responseOK(TraCIDataType.STRING_LIST, new ArrayList<>()));

		return rawCmd;
	}


	public TraCICommand processValueSub(TraCICommand rawCmd, RemoteManager remoteManager){
		return processValueSub(rawCmd, remoteManager, this::processGet,
				TraCICmd.SUB_VEHICLE_VALUE, TraCICmd.RESPONSE_SUB_VEHICLE_VALUE);
	}

	public TraCICommand processGet(TraCICommand cmd, RemoteManager remoteManager){
		TraCIGetCommand getCmd = (TraCIGetCommand) cmd;

		TraCIVehicleVar var = TraCIVehicleVar.fromId(getCmd.getVariableIdentifier());

		switch (var){
			case ID_LIST:
				return process_getIDList(getCmd, remoteManager, var);
			default:
				return process_UnknownCommand(getCmd, remoteManager);
		}
	}

}
