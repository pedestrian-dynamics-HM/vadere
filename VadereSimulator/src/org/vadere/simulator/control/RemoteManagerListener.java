package org.vadere.simulator.control;

public interface RemoteManagerListener {

	/**
	 *  Notify RemoteManger that the simulation reached end of loop and the {@link SimulationState}
	 *  is ready to be read/changed. The simulation thread will wait after the call is finished.
	 */
	void simulationStepFinishedListener();
}
