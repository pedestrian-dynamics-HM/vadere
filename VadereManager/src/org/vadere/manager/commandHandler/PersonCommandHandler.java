package org.vadere.manager.commandHandler;

import org.vadere.manager.TraCIException;
import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.commands.TraCIGetCommand;
import org.vadere.manager.stsc.commands.TraCISetCommand;

public class PersonCommandHandler extends CommandHandler{


	public static PersonCommandHandler instance;

	static {
		instance = new PersonCommandHandler();
	}

	private PersonCommandHandler(){

	}


	protected TraCIPacket process_getIDList(TraCIGetCommand cmd){



		TraCIPacket packet = TraCIPacket.createDynamicPacket();
		packet.add_OK_StatusResponse(cmd.getTraCICmd());

		// Test
		// get Data ...
//		List<String> list = new ArrayList<>();
//		packet.getWriter().writeCommandLength(4); // cmd leng
//		packet.getWriter().writeUnsignedByte(1);

		throw TraCIException.getNotImplemented(cmd);
	}

	protected TraCIPacket process_getIDCount(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	protected TraCIPacket process_getSpeed(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	protected TraCIPacket process_getPosition(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}


	protected TraCIPacket process_getLength(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}


	protected TraCIPacket process_getWidth(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	protected TraCIPacket process_getWaitingTime(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	public TraCIPacket processGet(TraCICommand cmd){
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
				return process_getWaitingTime(getCmd);
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

	public TraCIPacket processSet(TraCICommand cmd){
		TraCISetCommand setCmd = (TraCISetCommand) cmd;
		return null;
	}


}
