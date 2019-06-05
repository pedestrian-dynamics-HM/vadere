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


	protected TraCIPacket process_getPosition3D(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	protected TraCIPacket process_getAngle(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	protected TraCIPacket process_getSlope(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	protected TraCIPacket process_getRoadID(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	protected TraCIPacket process_getTypeID(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	protected TraCIPacket process_getColor(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}


	protected TraCIPacket process_getLanePosition(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}


	protected TraCIPacket process_Length(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	protected TraCIPacket process_MinGap(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	protected TraCIPacket process_getWidth(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	protected TraCIPacket process_getWaitingTime(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	protected TraCIPacket process_getNextEdge(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	protected TraCIPacket process_getRemainingStages(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	protected TraCIPacket process_getVehicle(TraCIGetCommand cmd){
		throw TraCIException.getNotImplemented(cmd);
	}

	public TraCIPacket processGet(TraCICommand cmd){
		TraCIGetCommand getCmd = (TraCIGetCommand) cmd;
//		switch (cmd.getVariableId()){
//			case TraCIVariable.ID_LIST.id:
//
//		}
		return null;
	}

	public TraCIPacket processSet(TraCICommand cmd){
		TraCISetCommand setCmd = (TraCISetCommand) cmd;
		return null;
	}


}
