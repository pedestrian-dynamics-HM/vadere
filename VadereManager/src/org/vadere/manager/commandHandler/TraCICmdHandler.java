package org.vadere.manager.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.stsc.commands.TraCICommand;

@FunctionalInterface
public interface TraCICmdHandler {


	TraCICommand handel(TraCICommand cmd, RemoteManager remoteManager);

}