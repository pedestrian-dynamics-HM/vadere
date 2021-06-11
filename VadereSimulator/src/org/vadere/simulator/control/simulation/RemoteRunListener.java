package org.vadere.simulator.control.simulation;

public interface RemoteRunListener {

	/**
	 *  Notify RemoteManger that the simulation reached end of loop and the {@link SimulationState}
	 *  is ready to be read/changed. The simulation thread will wait after the call is finished.
	 */
	void notifySimStepListener();

	void notifySimulationEndListener();

	/**
	 *  Notify RemoteManger about early shutdown. This is used to gracefully stop TraCI connection.
	 */
	void simulationStoppedEarlyListener(double time);
}
