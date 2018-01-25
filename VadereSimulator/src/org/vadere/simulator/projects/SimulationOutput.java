package org.vadere.simulator.projects;

import java.nio.file.Path;

/**
 * Represents the directory holding a simulation output
 *
 * @author Stefan Schuhbäck
 */
public class SimulationOutput {

	private Scenario simulatedScenario;private Path dir;
	private boolean isDirty;

	public SimulationOutput(Path directory, Scenario scenario){
		this.dir = directory;
		this.simulatedScenario = scenario;
		this.isDirty = false;
	}


	public Scenario getSimulatedScenario() {
		return simulatedScenario;
	}
}
