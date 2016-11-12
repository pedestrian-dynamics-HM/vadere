package org.vadere.simulator.entrypoints;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.nio.file.Paths;

public class ScenarioFactory {

	/**
	 * Create a new {@link Scenario} with the specified name based on the path
	 * to the directory of the project and the filename of the scenario.
	 * 
	 * @param projectdirectory directory to the addressed project
	 * @param name name of the new scenario
	 * @param fileName filename of the addressed scenario
	 */
	public static Scenario createVadereWithProjectDirectory(final String projectdirectory,
			final String fileName, final String name) throws IOException {
		String scenarioDir = IOUtils.SCENARIO_DIR;
		if (projectdirectory.endsWith(IOUtils.SCENARIO_DIR))
			scenarioDir = "";

		String json = IOUtils.readTextFile(Paths.get(projectdirectory, scenarioDir, fileName).toString());
		Scenario scenario = IOVadere.fromJson(json);

		return scenario;
	}

}
