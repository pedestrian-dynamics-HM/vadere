package org.vadere.manager.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.TraCIDataType;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.commands.TraCIGetCommand;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;

public class PersonCommandHandler extends CommandHandler{


	public static PersonCommandHandler instance;

	static {
		instance = new PersonCommandHandler();
	}

	private PersonCommandHandler(){

	}

	protected TraCICommand process_getIDList(TraCIGetCommand cmd, RemoteManager remoteManager){

		remoteManager.accessState(state -> {
			cmd.setResponseData(new ArrayList<>(state.getTopography().getPedestrianDynamicElements().getElements()));
		});

		cmd.setResponseDataType(TraCIDataType.STRING_LIST);
		cmd.setResponseIdentifier(TraCICmd.RESPONSE_GET_PERSON_VALUE);

		return cmd;
	}

	protected TraCICommand process_getIDCount(TraCIGetCommand cmd, RemoteManager remoteManager){

		int numPerson = 0; // get...

		cmd.addResponseIdentifier(TraCICmd.RESPONSE_GET_PERSON_VALUE)
				.addResponseVariableType(TraCIDataType.INTEGER)
				.addResponseData(numPerson);

		return cmd;

	}

	protected TraCICommand process_getSpeed(TraCIGetCommand cmd, RemoteManager remoteManager){

		double speed = 0.0; // get...

		cmd.addResponseIdentifier(TraCICmd.RESPONSE_GET_PERSON_VALUE)
				.addResponseVariableType(TraCIDataType.DOUBLE)
				.addResponseData(speed);
		return cmd;
	}

	protected TraCICommand process_getPosition(TraCIGetCommand cmd, RemoteManager remoteManager){

		VPoint point = new VPoint(1.0, 1.0); // get

		cmd.addResponseIdentifier(TraCICmd.RESPONSE_GET_PERSON_VALUE)
				.addResponseVariableType(TraCIDataType.POS_2D)
				.addResponseData(point);

		return cmd;
	}


	protected TraCICommand process_getLength(TraCIGetCommand cmd, RemoteManager remoteManager){


		double pedLength = 0.4; // get... (durchmesser)

		cmd.addResponseIdentifier(TraCICmd.RESPONSE_GET_PERSON_VALUE)
				.addResponseVariableType(TraCIDataType.DOUBLE)
				.addResponseData(pedLength);

		return cmd;
	}


	protected TraCICommand process_getWidth(TraCIGetCommand cmd, RemoteManager remoteManager){
		double pedWidth = 0.4; // get.. (durchmesser)

		cmd.addResponseIdentifier(TraCICmd.RESPONSE_GET_PERSON_VALUE)
				.addResponseVariableType(TraCIDataType.DOUBLE)
				.addResponseData(pedWidth);

		return cmd;
	}


	public TraCICommand processGet(TraCICommand cmd, RemoteManager remoteManager){
		TraCIGetCommand getCmd = (TraCIGetCommand) cmd;

		switch (TraCIPersonVar.fromId(getCmd.getVariableId())){
			case ID_LIST:
				return process_getIDList(getCmd, remoteManager);
			case COUNT:
				return process_getIDCount(getCmd, remoteManager);
			case SPEED:
				return process_getSpeed(getCmd, remoteManager);
			case POS_2D:
				return process_getPosition(getCmd, remoteManager);
			case LENGTH:
				return process_getLength(getCmd, remoteManager);
			case WIDTH:
				return process_getWidth(getCmd, remoteManager);
			case WAITING_TIME:
			case POS_3D:
			case ANGLE:
			case ROAD_ID:
			case TYPE:
			case COLOR:
			case EDGE_POS:
			case MIN_GAP:
			case NEXT_EDGE:
			case REMAINING_STAGES:
			case VEHICLE:
				return process_NotImplemented(getCmd, remoteManager);
			case UNKNOWN:
			default:
				return process_UnknownCommand(getCmd, remoteManager);
		}
	}

	public TraCICommand processSet(TraCICommand cmd, RemoteManager remoteManager){
		return process_NotImplemented(cmd, remoteManager);
	}


}
