package org.vadere.manager.commandHandler;

import org.vadere.manager.stsc.TraCIDataType;
import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.commands.TraCIGetCommand;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.List;

public class PersonCommandHandler extends CommandHandler{


	public static PersonCommandHandler instance;

	static {
		instance = new PersonCommandHandler();
	}

	private PersonCommandHandler(){

	}

	protected TraCICommand process_getIDList(TraCIGetCommand cmd){

		List<String> pedestrians = null; // get....

		cmd.addResponseIdentifier(TraCICmd.RESPONSE_GET_PERSON_VALUE)
				.addResponseVariableType(TraCIDataType.STRING_LIST)
				.addResponseData(pedestrians);
		return cmd;
	}

	protected TraCICommand process_getIDCount(TraCIGetCommand cmd){

		int numPerson = 0; // get...

		cmd.addResponseIdentifier(TraCICmd.RESPONSE_GET_PERSON_VALUE)
				.addResponseVariableType(TraCIDataType.INTEGER)
				.addResponseData(numPerson);

		return cmd;

	}

	protected TraCICommand process_getSpeed(TraCIGetCommand cmd){

		double speed = 0.0; // get...

		cmd.addResponseIdentifier(TraCICmd.RESPONSE_GET_PERSON_VALUE)
				.addResponseVariableType(TraCIDataType.DOUBLE)
				.addResponseData(speed);
		return cmd;
	}

	protected TraCICommand process_getPosition(TraCIGetCommand cmd){

		VPoint point = new VPoint(1.0, 1.0); // get

		cmd.addResponseIdentifier(TraCICmd.RESPONSE_GET_PERSON_VALUE)
				.addResponseVariableType(TraCIDataType.POS_2D)
				.addResponseData(point);

		return cmd;
	}


	protected TraCICommand process_getLength(TraCIGetCommand cmd){


		double pedLength = 0.4; // get... (durchmesser)

		cmd.addResponseIdentifier(TraCICmd.RESPONSE_GET_PERSON_VALUE)
				.addResponseVariableType(TraCIDataType.DOUBLE)
				.addResponseData(pedLength);

		return cmd;
	}


	protected TraCICommand process_getWidth(TraCIGetCommand cmd){
		double pedWidth = 0.4; // get.. (durchmesser)

		cmd.addResponseIdentifier(TraCICmd.RESPONSE_GET_PERSON_VALUE)
				.addResponseVariableType(TraCIDataType.DOUBLE)
				.addResponseData(pedWidth);

		return cmd;
	}


	public TraCICommand processGet(TraCICommand cmd){
		TraCIGetCommand getCmd = (TraCIGetCommand) cmd;

		switch (TraCIPersonVar.fromId(getCmd.getVariableId())){
			case ID_LIST:
				return process_getIDList(getCmd);
			case COUNT:
				return process_getIDCount(getCmd);
			case SPEED:
				return process_getSpeed(getCmd);
			case POS_2D:
				return process_getPosition(getCmd);
			case LENGTH:
				return process_getLength(getCmd);
			case WIDTH:
				return process_getWidth(getCmd);
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
				return process_NotImplemented(getCmd);
			case UNKNOWN:
			default:
				return process_UnknownCommand(getCmd);
		}
	}

	public TraCICommand processSet(TraCICommand cmd){
		return process_NotImplemented(cmd);
	}


}
