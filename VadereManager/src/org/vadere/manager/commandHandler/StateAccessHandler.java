package org.vadere.manager.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.simulator.control.SimulationState;

@FunctionalInterface
public interface StateAccessHandler {

	void execute(RemoteManager remoteManager, SimulationState state);

}
