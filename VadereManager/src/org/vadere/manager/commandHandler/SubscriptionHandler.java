package org.vadere.manager.commandHandler;

import org.vadere.manager.stsc.commands.TraCIGetCommand;

import java.util.List;

@FunctionalInterface
public interface SubscriptionHandler {

	void handel(List<TraCIGetCommand> getCommands);

}
