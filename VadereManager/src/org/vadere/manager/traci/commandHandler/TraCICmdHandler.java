package org.vadere.manager.traci.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.traci.commands.TraCICommand;

/**
 * Interface used to dispatch command handling to the correct {@link CommandHandler} subclass
 */
@FunctionalInterface
public interface TraCICmdHandler {

	TraCICommand handel(TraCICommand cmd, RemoteManager remoteManager);

}
