package org.vadere.manager.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.respons.TraCIStatusResponse;
import org.vadere.manager.stsc.commands.TraCICommand;

public abstract class CommandHandler {


	public TraCICommand process_NotImplemented(TraCICommand cmd, RemoteManager remoteManager){
		return cmd.setNOK_response(TraCIPacket.sendStatus(cmd.getTraCICmd(),
				TraCIStatusResponse.NOT_IMPLEMENTED,
				"Command " + cmd.getCmdType().toString() + "not Implemented"));
	}

	public TraCICommand process_UnknownCommand(TraCICommand cmd, RemoteManager remoteManager){
		return cmd.setNOK_response(TraCIPacket.sendStatus(cmd.getTraCICmd(),
				TraCIStatusResponse.ERR,
				"Command " + cmd.getCmdType().toString() + "Unknown"));
	}
}
