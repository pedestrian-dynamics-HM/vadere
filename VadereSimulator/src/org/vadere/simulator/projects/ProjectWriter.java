package org.vadere.simulator.projects;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.util.io.IOUtils;

/**
 * Writes a {@link VadereProject} to a file.
 * 
 * 
 */
public class ProjectWriter {

	/**
	 * Writes a {@link VadereProject} to file.
	 * 
	 * @param folderpath
	 *        path of the project file to write.
	 * @param project
	 *        project to write.
	 * @throws IOException
	 *         if something goes wrong writing the file.
	 */
	public static void writeProjectFileJson(String folderpath, VadereProject project) throws IOException {
		writeProjectFileJson(folderpath, project, false, false);
	}

	/**
	 * Writes a {@link VadereProject} to file.
	 *
	 * @param folderpath
	 *        path of the project file to write.
	 * @param project
	 *        project to write.
	 * @param override
	 *        if true => if directories already exists they will be used,
	 *        if false => if directories already exist this method throws an
	 *        {@link java.io.IOException}
	 * @throws IOException
	 *         if something goes wrong writing the file (for example override == false and a
	 *         directory already exists).
	 */
	public static void writeProjectFileJson(final String folderpath, final VadereProject project, boolean override,
			boolean includeCommitHash) throws IOException {
		Path projectPath = Paths.get(folderpath);
		Path scenarioPath = Paths.get(folderpath, IOUtils.SCENARIO_DIR);
		Path outputDir = Paths.get(folderpath, IOUtils.OUTPUT_DIR);

		if (!override || !Files.exists(projectPath)) {
			Files.createDirectories(projectPath);
		}

		if (!override || !Files.exists(scenarioPath)) {
			Files.createDirectories(scenarioPath);
		}

		if (!override || !Files.exists(outputDir)) {
			Files.createDirectories(outputDir);
		}

		project.setOutputDir(outputDir);

		Path filepath = Paths.get(projectPath.toString(), IOUtils.VADERE_PROJECT_FILENAME);
		Files.deleteIfExists(filepath);
		IOUtils.writeTextFile(filepath.toString(), project.getName());

		// create all scenario files, try to save as many as possible!

		for (Scenario scenario : project.getScenarios()) {
			IOUtils.writeTextFile(
					getScenarioPath(scenarioPath, scenario).toString(),
					JsonConverter.serializeScenarioRunManager(scenario, includeCommitHash));
		}
	}

	public static void writeScenarioFileJson(final String projectFolderPath, Scenario scenario)
			throws IOException {
		Path scenariosDir = Files.createDirectories(Paths.get(projectFolderPath, IOUtils.SCENARIO_DIR));
		IOUtils.writeTextFile(
				getScenarioPath(scenariosDir, scenario).toString(),
				JsonConverter.serializeScenarioRunManager(scenario, true));
	}

	public static Path getScenarioPath(Path scenariosDir, Scenario scenario) {
		return Paths.get(scenariosDir.toString(), scenario.getName() + IOUtils.SCENARIO_FILE_EXTENSION);
	}

	public static void deleteScenario(Scenario scenario, String folderpath) throws IOException {
		Path scenariosDir = Files.createDirectories(Paths.get(folderpath, IOUtils.SCENARIO_DIR));
		Files.delete(getScenarioPath(scenariosDir, scenario));
	}

	public static void deleteOutput(String pathsName) throws IOException {
		Files.delete(Paths.get(pathsName));
	}

	public static void renameOutput(final String oldPath, final String newPath) throws IOException {
		Files.move(Paths.get(oldPath), Paths.get(newPath));
	}

	public static void renameScenario(Scenario scenario, String folderpath, String newName)
			throws IOException {
		Path scenariosDir = Files.createDirectories(Paths.get(folderpath, IOUtils.SCENARIO_DIR));
		Path scenarioPath = getScenarioPath(scenariosDir, scenario);
		scenario.setName(newName);
		Path targetPath = getScenarioPath(scenariosDir, scenario);
		Files.move(scenarioPath, targetPath);
	}

	public static String getProjectDir(final String projectFilePath) {
		Path p = Paths.get(projectFilePath);
		if (Files.exists(p) && !Files.isDirectory(p)) {
			p = p.getParent();
		}
		return p.toString();
	}
}
