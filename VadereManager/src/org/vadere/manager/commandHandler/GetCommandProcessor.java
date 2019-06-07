package org.vadere.manager.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.commands.TraCIGetCommand;

public interface GetCommandProcessor {

	TraCICommand process(TraCIGetCommand cmd, RemoteManager remoteManager);
}
