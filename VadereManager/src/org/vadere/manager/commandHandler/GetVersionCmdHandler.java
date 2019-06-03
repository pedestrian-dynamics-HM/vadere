package org.vadere.manager.commandHandler;


import de.tudresden.sumo.config.Constants;

import org.vadere.manager.TraCICommand;
import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.TraCIWriter;

public class GetVersionCmdHandler implements CommandHandler{


	@Override
	public TraCIPacket handelCommand(TraCICommand cmd) {


		TraCIPacket response = TraCIPacket.createDynamicPacket();

		response.add_OK_StatusResponse(Constants.CMD_GETVERSION);

		TraCIWriter writer = response.getWriter();

		int cmdLen = 10 + writer.stringByteCount("Vaderer TraCI Server");
		writer.writeCommandLength(cmdLen);	// 1b or 5b
		writer.writeInt(Constants.CMD_GETVERSION); // 1b
		writer.writeUnsignedByte(Constants.TRACI_VERSION); // 4b
		writer.writeString("Vaderer TraCI Server"); // 4b + X

		return response;
	}
}