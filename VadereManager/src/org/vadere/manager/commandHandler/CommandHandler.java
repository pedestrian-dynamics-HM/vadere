package org.vadere.manager.commandHandler;

import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.commands.TraCICommand;

public abstract class CommandHandler {


	public TraCIPacket process_NotImplemented(TraCICommand cmd){
		return null;
	}

	public TraCIPacket process_UnknownCommand(TraCICommand cmd){
		return null;
	}
}
