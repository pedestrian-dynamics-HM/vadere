package org.vadere.simulator.entrypoints;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The VadereFactory create new Vadere-Scenario objects.
 *
 */
public class VadereFactory {

	private static Logger logger = LogManager.getLogger(VadereFactory.class);

	/**
	 * Create a new Vadere with the specified name based on the path to the directory of the project
	 * and the filename of the scenario of the Vadere.
	 * 
	 * @param projectdirectory directory to the addressed project
	 * @param name name of the new Vadere object
	 * @param fileName filename of the addressed scenario
	 * @return a new Vadere object
	 * @throws IOException if something goes wrong creatin the output folders of the project
	 */
	public static ScenarioRunManager createVadereWithProjectDirectory(final String projectdirectory,
			final String fileName, final String name) throws IOException {
		String scenarioDir = IOUtils.SCENARIO_DIR;
		if (projectdirectory.endsWith(IOUtils.SCENARIO_DIR))
			scenarioDir = "";

		String json = IOUtils.readTextFile(Paths.get(projectdirectory, scenarioDir, fileName).toString());
		ScenarioRunManager v = IOVadere.fromJson(json);

		Path outPath = Paths.get(projectdirectory, IOUtils.OUTPUT_DIR);
		if (!Files.exists(outPath))
			Files.createDirectories(outPath);
		v.setOutputPaths(outPath);

		return v;
	}

}
