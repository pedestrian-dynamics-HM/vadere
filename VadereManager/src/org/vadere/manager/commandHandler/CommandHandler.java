package org.vadere.manager.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.respons.TraCIStatusResponse;
import org.vadere.manager.stsc.commands.TraCICommand;


/**
 * {@link CommandHandler} classes perform the actual request within the command.
 *
 * See {@link CommandExecutor} on how commands are dispatched to the correct {@link CommandHandler}
 * subclass. These classes implement methods which adhere to the TraCICmdHandler Interface. These
 * methods are used by the {@link CommandExecutor} for dispatching.
 *
 */
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
