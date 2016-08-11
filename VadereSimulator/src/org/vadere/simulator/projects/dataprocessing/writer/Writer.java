package org.vadere.simulator.projects.dataprocessing.writer;

import org.vadere.simulator.control.SimulationState;

public interface Writer {

	public void preLoop(final SimulationState state);

	public void postLoop(final SimulationState state);

	public void update(final SimulationState state);
}

