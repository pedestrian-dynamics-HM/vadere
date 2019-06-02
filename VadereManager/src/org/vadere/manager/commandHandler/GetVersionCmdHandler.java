package org.vadere.manager.commandHandler;


import de.tudresden.sumo.config.Constants;

import org.vadere.manager.TraCiCommandBuilder;
import org.vadere.manager.TraCiMessageBuilder;
import org.vadere.manager.TraciCommand;

public class GetVersionCmdHandler implements CommandHandler{


	@Override
	public boolean handelCommand(TraciCommand cmd, TraCiMessageBuilder builder) {


		writeStatusCmd(builder, Constants.CMD_GETVERSION, Constants.RTYPE_OK, "");

		TraCiCommandBuilder b = new TraCiCommandBuilder();

//		b.writeUnsignedByte(Constants.CMD_GETVERSION);
		b.writeUnsignedByte(0x33);
		b.writeStringASCII("Vaderer TraCI Server");

		builder.writeBytes(b.build());

		return true;
	}
}