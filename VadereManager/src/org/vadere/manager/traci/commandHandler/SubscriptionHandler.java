package org.vadere.manager.traci.commandHandler;

import org.vadere.manager.traci.commands.TraCIGetCommand;

import java.util.List;

@FunctionalInterface
public interface SubscriptionHandler {

	void handel(List<TraCIGetCommand> getCommands);

}
