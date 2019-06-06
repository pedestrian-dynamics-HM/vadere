package org.vadere.manager.commandHandler;

import org.vadere.simulator.control.SimulationState;

@FunctionalInterface
public interface StateAccessHandler {

	void execute(SimulationState state);

}
