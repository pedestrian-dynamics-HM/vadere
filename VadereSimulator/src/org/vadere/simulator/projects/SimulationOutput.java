package org.vadere.simulator.projects;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.nio.file.Path;

/**
 * Represents the directory holding a simulation output
 *
 * @author Stefan Schuhbäck
 */
public class SimulationOutput {

	private final Scenario simulatedScenario;
	private final Path outputDirectory;
	private final String scenarioHash;
	private boolean isDirty;


	public SimulationOutput(Path directory, Scenario scenario) {
		this.outputDirectory = directory;
		this.simulatedScenario = scenario;
		this.isDirty = false;
		String tmpHash;
		try {
			tmpHash = scenario.getScenarioStore().hashOfJsonRepresentation();
		} catch (JsonProcessingException e) {
			tmpHash = "";
			e.printStackTrace();
		}
		this.scenarioHash = tmpHash;
	}

	public String getScenarioHash() {
		return this.scenarioHash;
	}

	public Scenario getSimulatedScenario() {
		return simulatedScenario;
	}

	public boolean isDirty() {
		return isDirty;
	}

	public void setDirty() {
		isDirty = true;
	}

}
