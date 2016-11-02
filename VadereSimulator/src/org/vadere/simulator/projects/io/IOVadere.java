package org.vadere.simulator.projects.io;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.migration.MigrationAssistant;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.io.IOUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IOVadere {

	private static Logger logger = LogManager.getLogger(IOVadere.class);

	public static ScenarioRunManager fromJson(final String json) throws IOException {
		return StateJsonConverter.deserializeScenarioRunManager(json);
	}

	public static VadereProject readProjectJson(final String filepath)
			throws ParserConfigurationException, SAXException,
			IOException, TransformerException {

		Path p = Paths.get(filepath);
		if (!Files.isDirectory(p))
			p = p.getParent();

		return IOVadere.readProject(p.toString());
	}

	public static VadereProject readProject(final String folderpath) throws IOException {
		String name = IOUtils.readTextFile(Paths.get(folderpath, IOUtils.VADERE_PROJECT_FILENAME).toString());
		List<ScenarioRunManager> scenarios = new ArrayList<>();
		Set<String> scenarioNames = new HashSet<>();
		Path p = Paths.get(folderpath, IOUtils.SCENARIO_DIR);
		int[] migrationStats = {0, 0, 0};
		if (Files.isDirectory(p)) {

			migrationStats = MigrationAssistant.analyzeProject(folderpath);

			for (File file : IOUtils.getFilesInScenarioDirectory(p)) {
				ScenarioRunManager scenario =
						StateJsonConverter.deserializeScenarioRunManager(IOUtils.readTextFile(file.getAbsolutePath()));
				if (!scenarioNames.add(scenario.getName())) {
					logger.error("there are two scenarios with the same name!");
					throw new IOException("Found two scenarios with the same name.");
				}
				scenarios.add(scenario);
			}
		}

		VadereProject project = new VadereProject(name, scenarios);
		project.setMigrationStats(migrationStats); // TODO [priority=low] [task=refactoring] better way to tunnel those results to the GUI?
		project.setOutputDir(Paths.get(folderpath, IOUtils.OUTPUT_DIR));
		return project;
	}

}
