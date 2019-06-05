package org.vadere.manager.commandHandler;


import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.commands.TraCICommand;

public class ControlCommandHandler extends CommandHandler{

	public static ControlCommandHandler instance;

	static {
		instance = new ControlCommandHandler();
	}

	private ControlCommandHandler(){}

	public TraCIPacket process_getVersion(TraCICommand cmd) {

		TraCIPacket response = TraCIPacket.createDynamicPacket();

		//	@Override
//	public TraCIPacket handleCommand(TraCIPacket response) {
//		response = TraCIPacket.createDynamicPacket();
//
//		response.add_OK_StatusResponse(Constants.CMD_GETVERSION);
//
//		TraCIWriter writer = response.getWriter();
//
//		int cmdLen = 10 + writer.stringByteCount("Vaderer TraCI Server");
//		writer.writeCommandLength(cmdLen);	// 1b or 5b
//		writer.writeInt(Constants.CMD_GETVERSION); // 1b
//		writer.writeUnsignedByte(Constants.TRACI_VERSION); // 4b
//		writer.writeString("Vaderer TraCI Server"); // 4b + X
//
//		return response;
//	}

//		response.add_OK_StatusResponse(Constants.CMD_GETVERSION);
//
//		TraCIWriter writer = response.getWriter();
//
//		int cmdLen = 10 + writer.stringByteCount("Vaderer TraCI Server");
//		writer.writeCommandLength(cmdLen);	// 1b or 5b
//		writer.writeInt(Constants.CMD_GETVERSION); // 1b
//		writer.writeUnsignedByte(Constants.TRACI_VERSION); // 4b
//		writer.writeString("Vaderer TraCI Server"); // 4b + X

		return response;
	}


}