package org.vadere.simulator.projects.io;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.ProjectOutput;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.migration.MigrationAssistant;
import org.vadere.simulator.projects.migration.incidents.ExceptionIncident;
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

	public static Scenario fromJson(final String json) throws IOException, IllegalArgumentException {
		return JsonConverter.deserializeScenarioRunManager(json);
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
		logger.info("read .project file");

		List<Scenario> scenarios = new ArrayList<>();
		Set<String> scenarioNames = new HashSet<>();
		Path p = Paths.get(folderpath, IOUtils.SCENARIO_DIR);
		int[] migrationStats = {0, 0, 0};
		if (Files.isDirectory(p)) {

			migrationStats = MigrationAssistant.analyzeProject(folderpath);
            logger.info("analysed .scenario files");
			for (File file : IOUtils.getFilesInScenarioDirectory(p)) {
				try {
					Scenario scenario =
							JsonConverter.deserializeScenarioRunManager(IOUtils.readTextFile(file.getAbsolutePath()));
					if (!scenarioNames.add(scenario.getName())) {
						logger.error("there are two scenarios with the same name!");
						throw new IOException("Found two scenarios with the same name.");
					}
					scenarios.add(scenario);
				}
				catch (Exception e) {
					logger.error("could not read " + file.getName());
					throw e;
				}
			}
		}

		VadereProject project = new VadereProject(name, scenarios);
		project.setMigrationStats(migrationStats); // TODO [priority=low] [task=refactoring] better way to tunnel those results to the GUI?
		project.setOutputDir(Paths.get(folderpath, IOUtils.OUTPUT_DIR));
		ProjectOutput projectOutput = new ProjectOutput(project);
		project.setProjectOutput(projectOutput);
        logger.info("project loaded: " + project.getName());
		return project;
	}

}
