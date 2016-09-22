package org.vadere.simulator.projects.io;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.state.scenario.Agent;
import org.vadere.state.simulation.Step;
import org.vadere.util.io.IOUtils;
import org.vadere.util.reflection.VadereClassNotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This IOUtility class provides all methods to load, delete, list, clean output directories.
 * Each output directory contains two fiels *.scenario and *.trajectories.
 *
 */
public abstract class IOOutput {

	private static Logger logger = LogManager.getLogger(IOOutput.class);

	public static List<File> listSelectedOutputDirs(final VadereProject project, final ScenarioRunManager scenario) {
		List<File> selectedOutputDirectories = new LinkedList<>();

		selectedOutputDirectories = listAllOutputDirs(project).stream()
				.filter(dir -> isMatchingOutputDirectory(project, dir, scenario))
				.collect(Collectors.toList());

		return selectedOutputDirectories;
	}

	/**
	 * Returns all valid output directories of the project.
	 */
	public static List<File> listAllOutputDirs(final VadereProject project) {
		return listAllDirs(project).stream().filter(f -> isValidOutputDirectory(project, f))
				.collect(Collectors.toList());
	}

	/**
	 * Moves all invalid output directories to the corrupted directory
	 */
	public static void cleanOutputDirs(final VadereProject project) {
		listAllDirs(project).stream().filter(f -> !isValidOutputDirectory(project, f))
				.forEach(dir -> cleanDir(project, dir));
	}

	public static Map<Step, List<Agent>> readTrajectories(final VadereProject project,
			final ScenarioRunManager scenario, final String directoryName) throws IOException {
		TrajectoryReader reader = new TrajectoryReader(
				getPathToOutputFile(project, directoryName, IOUtils.TRAJECTORY_FILE_EXTENSION), scenario);
		return reader.readFile();
	}

	public static Map<Step, List<Agent>> readTrajectories(final Path trajectoryFilePath,
			final ScenarioRunManager scenario) throws IOException {
		TrajectoryReader reader = new TrajectoryReader(trajectoryFilePath, scenario);
		Map<Step, List<Agent>> result = reader.readFile();
		return result;
	}

	public static ScenarioRunManager readScenarioRunManager(final VadereProject project, final String directoryName)
			throws IOException {
		String snapshotString = IOUtils
				.readTextFile(getPathToOutputFile(project, directoryName, IOUtils.SCENARIO_FILE_EXTENSION).toString());
		return IOVadere.fromJson(snapshotString);
	}

	public static ScenarioRunManager readVadere(final File file) throws IOException {
		String snapshotString;
		Path path = file.toPath();
		if (file.isFile() && file.getName().endsWith(IOUtils.SCENARIO_FILE_EXTENSION)) {
			snapshotString = IOUtils.readTextFile(path.toString());
		} else if (file.isDirectory()) {
			Optional<File> scenarioFile = IOUtils.getFirstFile(path.toFile(), IOUtils.SCENARIO_FILE_EXTENSION);
			if (scenarioFile.isPresent()) {
				snapshotString = IOUtils.readTextFile(scenarioFile.get().toString());
			} else {
				throw new IOException("could not find scenario file: " + path.toString());
			}
		} else {
			throw new IOException(
					path.toString() + " is neither a *." + IOUtils.SCENARIO_FILE_EXTENSION + " file nor a directory");
		}
		return IOVadere.fromJson(snapshotString);
	}

	public static ScenarioRunManager readVadere(final Path path) throws IOException {
		return IOOutput.readVadere(path.toFile());
	}

	public static boolean renameOutputDirectory(final File directory, final String newName) {
		File newDirectory = directory.getParentFile().toPath().resolve(newName).toFile();

		if (directory.isDirectory() && !newDirectory.exists()) {
			directory.renameTo(newDirectory);
			return true;
		}
		return false;
	}

	public static boolean deleteOutputDirectory(final File directory) {
		boolean everythingDeleted = false;
		if (directory.isDirectory()) {
			File[] scenarioFile = IOUtils.getFileList(directory, IOUtils.SCENARIO_FILE_EXTENSION);
			File[] trajectoryFile = IOUtils.getFileList(directory, IOUtils.TRAJECTORY_FILE_EXTENSION);

			for (File file : scenarioFile) {
				try {
					Files.delete(file.toPath());
				} catch (IOException e) {
					logger.error("could not delete scenario-file: " + file.getAbsolutePath() + ", "
							+ e.getLocalizedMessage());
				}
			}

			for (File file : trajectoryFile) {
				try {
					Files.delete(file.toPath());
				} catch (IOException e) {
					logger.error("could not delete trajectory-file: " + file.getAbsolutePath() + ", "
							+ e.getLocalizedMessage());
				}
			}

			try {
				Files.delete(directory.toPath());
			} catch (IOException e) {
				logger.error("could not delete output-directory: " + directory.getAbsolutePath() + ", "
						+ e.getLocalizedMessage());
			}

			everythingDeleted = true;
		}

		return everythingDeleted;
	}


	private static List<File> listAllDirs(final VadereProject project) {
		List<File> outputDirectories = new LinkedList<>();
		if (Files.exists(project.getOutputDir())) {
			File[] files = new File(project.getOutputDir().toString()).listFiles(f -> f.isDirectory());
			if (files != null) {
				outputDirectories = Arrays.stream(files).filter(dir -> !dir.getName().equals(IOUtils.CORRUPT_DIR))
						.collect(Collectors.toList());
			}
		}
		return outputDirectories;
	}

	private static void cleanDir(final VadereProject project, final File directory) {
		IOUtils.errorBox(
				"The directory '"
						+ directory.getName()
						+ "' is corrupted and was moved to the '" + IOUtils.CORRUPT_DIR + "' folder.",
				"Corrupt output file detected.");
		try {
			Files.createDirectories(Paths.get(project.getOutputDir().toString(), IOUtils.CORRUPT_DIR));
			Path sourcePath = directory.toPath();
			Path targetPath = Paths.get(project.getOutputDir().toString(), IOUtils.CORRUPT_DIR, directory.getName());
			Files.move(sourcePath, targetPath, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e1) {
			logger.error(e1);
		}
	}

	private static Optional<ScenarioRunManager> readOutputFile(final VadereProject project, final File directory) {
		Optional<ScenarioRunManager> optionalVadere = Optional.empty();
		Path pathToSnapshot;
		try {
			pathToSnapshot = getPathToOutputFile(project, directory.getName(), IOUtils.SCENARIO_FILE_EXTENSION);
			optionalVadere = Optional.of(IOVadere.fromJson(IOUtils.readTextFile(pathToSnapshot.toString())));
		} catch (IOException | VadereClassNotFoundException e) {
			optionalVadere = Optional.empty();
			logger.error("Error in output file " + directory.getName());
		}
		return optionalVadere;
	}

	private static boolean isValidOutputDirectory(final VadereProject project, final File directory) {
		return readOutputFile(project, directory).isPresent();
	}

	private static boolean isMatchingOutputDirectory(final VadereProject project, final File directory,
			final ScenarioRunManager scenario) {
		Optional<ScenarioRunManager> optionalScenario = readOutputFile(project, directory);
		return directory.isDirectory() && optionalScenario.isPresent() && equalHash(optionalScenario.get(), scenario);
	}

	private static boolean equalHash(final ScenarioRunManager scenario1, ScenarioRunManager scenario2) {
		try {
			final String hash1 = scenario1.getScenarioStore().hashOfJsonRepresentation();
			final String hash2 = scenario2.getScenarioStore().hashOfJsonRepresentation();
			return hash1.equals(hash2);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return false;
		}
	}

	private static Path getPathToOutputFile(final VadereProject project, final String directoryName,
			final String fileExtension) throws IOException {
		return IOOutput.getPathToOutputFile(project.getOutputDir(), directoryName, fileExtension);
	}

	private static Path getPathToOutputFile(final Path outputDir, final String directoryName,
			final String fileExtension) throws IOException {
		Path dir = outputDir.resolve(directoryName);

		File[] files = new File(dir.toString()).listFiles((d, name) -> name.toLowerCase().endsWith(fileExtension));

		if (files == null || files.length < 1) {
			throw new IOException("missing trajectory file with the extension " + fileExtension);
		} else if (files.length > 1) {
			throw new IOException("multiply trajectory files with the extension " + fileExtension);
		} else {
			return files[0].toPath();
		}
	}
}
