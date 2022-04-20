package org.vadere.simulator.projects.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.vadere.simulator.projects.ProjectOutput;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.migration.MigrationAssistant;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.MigrationOptions;
import org.vadere.simulator.projects.migration.MigrationResult;
import org.vadere.simulator.projects.migration.jsontranformation.JsonMigrationAssistant;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;
import org.vadere.util.version.Version;

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

	private static Logger logger = Logger.getLogger(IOVadere.class);

	public static Scenario fromJson(String json) throws IOException, IllegalArgumentException {
		try {
			JsonMigrationAssistant migrationAssistant = (JsonMigrationAssistant) MigrationAssistant.getNewInstance(MigrationOptions.defaultOptions());
			String migrationResult = migrationAssistant.migrateScenarioFile(json, Version.latest());
			if (migrationResult != null){
				json = migrationResult;
			}
		}
		catch (MigrationException migrationException){
			migrationException.printStackTrace();
		}
		catch (Exception e) {
			logger.warn("could not deserialize " + json);
			throw e;
		}

		return JsonConverter.deserializeScenarioRunManager(json);

	}

	public static VadereProject readProjectJson(final String filepath) throws IOException {
		return readProjectJson(filepath, MigrationOptions.defaultOptions());
	}

	public static VadereProject readProjectJson(final String filepath, final MigrationOptions options)
			throws IOException {

		Path p = Paths.get(filepath);
		if (!Files.isDirectory(p))
			p = p.getParent();

		return IOVadere.readProject(p.toString(), options);
	}

	public static VadereProject readProject(final String folderpath) throws IOException {
		return readProject(folderpath, MigrationOptions.defaultOptions());
	}

	public static VadereProject readProject(final String projectPath, final MigrationOptions options) throws IOException {
		String path = Paths.get(projectPath, IOUtils.VADERE_PROJECT_FILENAME).toString();
		String name = IOUtils.readTextFile(path);
		logger.info(String.format("read project: %s", path));

		List<Scenario> scenarios = new ArrayList<>();
		Set<String> scenarioNames = new HashSet<>();
		Path p = Paths.get(projectPath, IOUtils.SCENARIO_DIR);
		MigrationResult migrationStats = new MigrationResult();
		if (Files.isDirectory(p)) {

			MigrationAssistant migrationAssistant = MigrationAssistant.getNewInstance(options);
			migrationStats = migrationAssistant.analyzeProject(projectPath);
			logger.info("analyzed scenario files.");

			for (File file : IOUtils.getFilesInScenarioDirectory(p)) {
				try {
					Scenario scenario =
							JsonConverter.deserializeScenarioRunManager(IOUtils.readTextFile(file.getAbsolutePath()));
					if (!scenarioNames.add(scenario.getName())) {
						String errorMessage = String.format("There are two scenarios with the same name: %s\nConflicting file: %s",
								scenario.getName(), file.getAbsolutePath());
						logger.error(errorMessage);
						throw new IOException(errorMessage);
					}
					scenarios.add(scenario);
				} catch (Exception e) {
					logger.error("could not read " + file.getAbsolutePath());
					throw e;
				}
			}
		}

		VadereProject project = new VadereProject(name, scenarios, Paths.get(projectPath));
		logger.info(migrationStats.toString());
		project.setMigrationStats(migrationStats); // TODO [priority=low] [task=refactoring] better way to tunnel those results to the GUI?
		project.setOutputDir(Paths.get(projectPath, IOUtils.OUTPUT_DIR));
		ProjectOutput projectOutput = new ProjectOutput(project);
		project.setProjectOutput(projectOutput);
		logger.info("project loaded: " + project.getName());
		return project;
	}

}
