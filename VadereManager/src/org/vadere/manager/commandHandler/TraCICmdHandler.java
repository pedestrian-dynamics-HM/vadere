package org.vadere.manager.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.stsc.commands.TraCICommand;

/**
 * Interface used to dispatch command handling to the correct {@link CommandHandler} subclass
 */
@FunctionalInterface
public interface TraCICmdHandler {

	TraCICommand handel(TraCICommand cmd, RemoteManager remoteManager);

}
