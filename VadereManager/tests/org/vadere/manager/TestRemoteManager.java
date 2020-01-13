package org.vadere.manager;


import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.vadere.manager.traci.commandHandler.StateAccessHandler;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.types.ScenarioElementType;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Simplified version of RemoteManager used to test ONLY {@link SimulationState} access during calls
 * to {@link #accessState(StateAccessHandler)} by some CommandHandler. Instantiate new anonymous
 * classes of this abstract base and implement {@link #mockIt()} to mock the #SimulationState for
 * the {@link #accessState(StateAccessHandler)} call.
 */
public abstract class TestRemoteManager extends RemoteManager {

	protected SimulationState simState;
	protected RemoteManager remoteManager;

	public TestRemoteManager() {
		super(Paths.get(""), false);
		this.simState = mock(SimulationState.class, Mockito.RETURNS_DEEP_STUBS);
		this.remoteManager = mock(RemoteManager.class, Mockito.RETURNS_DEEP_STUBS);
		mockIt();
	}

	@Override
	public boolean accessState(StateAccessHandler stateAccessHandler) {
		stateAccessHandler.execute(remoteManager, simState);
		return true;
	}

	protected abstract void mockIt();

}




