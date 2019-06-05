package org.vadere.manager.commandHandler;

import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.TraCIStatusResponse;
import org.vadere.manager.stsc.commands.TraCICommand;

public abstract class CommandHandler {


	public TraCIPacket process_NotImplemented(TraCICommand cmd){
		return TraCIPacket.sendStatus(cmd.getTraCICmd(), TraCIStatusResponse.NOT_IMPLEMENTED, "Command " + cmd.getCmdType().toString() + "not Implemented");
	}

	public TraCIPacket process_UnknownCommand(TraCICommand cmd){
		return TraCIPacket.sendStatus(cmd.getTraCICmd(), TraCIStatusResponse.ERR, "Command " + cmd.getCmdType().toString() + "Unknown");
	}
}
