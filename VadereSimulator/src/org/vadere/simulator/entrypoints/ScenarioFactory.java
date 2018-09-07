package org.vadere.simulator.entrypoints;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ScenarioFactory {

	/**
	 * Create a new {@link Scenario} with the specified name based on the path
	 * to the directory of the project and the filename of the scenario.
	 * 
	 * @param projectdirectory directory to the addressed project
	 * @param fileName filename of the addressed scenario
	 */
	public static Scenario createVadereWithProjectDirectory(
			final String projectdirectory,
			final String fileName) throws IOException {

		String scenarioDir = IOUtils.SCENARIO_DIR;

		if (projectdirectory.endsWith(IOUtils.SCENARIO_DIR))
			scenarioDir = "";

		String json = IOUtils.readTextFile(Paths.get(projectdirectory, scenarioDir, fileName).toString());
		Scenario scenario = IOVadere.fromJson(json);

		return scenario;
	}


	public static Scenario createScenarioWithScenarioFilePath(
			final Path scenarioPath) throws IOException {

		String json = IOUtils.readTextFile(scenarioPath);
		Scenario scenario = IOVadere.fromJson(json);

		return scenario;
	}

	public static Scenario createScenarioWithScenarioJson(
			final String scenarioJson) throws IOException {

		Scenario scenario = IOVadere.fromJson(scenarioJson);

		return scenario;
	}
}
