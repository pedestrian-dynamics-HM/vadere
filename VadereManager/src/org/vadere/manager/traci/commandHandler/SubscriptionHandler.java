package org.vadere.manager.traci.commandHandler;

import java.util.List;
import org.vadere.manager.traci.commands.TraCIGetCommand;

@FunctionalInterface
public interface SubscriptionHandler {

  void handel(List<TraCIGetCommand> getCommands);
}
