package org.vadere.manager.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.simulator.control.SimulationState;

/**
 * Interface used to allow access to the {@link SimulationState}
 *
 * This Interface is only implemented as lambda functions in {@link CommandHandler}s
 * (see {@link PersonCommandHandler} for usage)
 *
 * In {@link RemoteManager#accessState(StateAccessHandler)} the access to the state is
 * managerd and monitored to ensure that {@link SimulationState} access is synchronized
 * and only occurs if the simulation is halted.
 *
 * (see singleStepMode in {@link org.vadere.simulator.control.Simulation})
 * 
 */
@FunctionalInterface
public interface StateAccessHandler {

	void execute(RemoteManager remoteManager, SimulationState state);

}
