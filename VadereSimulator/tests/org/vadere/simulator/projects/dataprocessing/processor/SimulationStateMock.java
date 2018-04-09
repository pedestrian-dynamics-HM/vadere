package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.control.SimulationState;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Abstract Class for {@link SimulationStateMock}s
 *
 * @author Stefan Schuhbäck
 */
public abstract class SimulationStateMock {

	protected SimulationState state;

	SimulationStateMock() {
		this.state = mock(SimulationState.class, Mockito.RETURNS_DEEP_STUBS);
		mockIt();
	}

	SimulationStateMock(int simStep) {
		this.state = mock(SimulationState.class, Mockito.RETURNS_DEEP_STUBS);
		when(state.getStep()).thenReturn(simStep);
		mockIt();
	}

	/**
	 * Define a single {@link SimulationState} an what should be mocked within in. Use within {@link
	 * ProcessorTestEnv#loadDefaultSimulationStateMocks()} to set a sequence of {@link
	 * SimulationState} for a specific {@link DataProcessor} testcase
	 */
	public abstract void mockIt();
}
