package org.vadere.simulator.projects.io;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.SimulationOutput;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;
import org.vadere.util.reflection.VadereClassNotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import tech.tablesaw.api.Table;

/**
 * This IOUtility class provides all methods to loadFromFilesystem, delete, list, clean output directories.
 * Each output directory contains two files *.scenario and *.trajectories.
 *
 */
public abstract class IOOutput {

	private static final Logger logger = Logger.getLogger(IOOutput.class);

	public static List<File> listSelectedOutputDirs(final VadereProject project, final Scenario scenario) {

		List<File> selectedOutputDirectories = listAllOutputDirs(project).stream()
				.filter(dir -> isMatchingOutputDirectory(project, dir, scenario))
				.collect(Collectors.toList());

		return selectedOutputDirectories;
	}

	/**
	 * Returns all valid output directories of the project.
	 */
	public static List<File> listAllOutputDirs(final VadereProject project) {
		return listAllDirs(project).stream().filter(f -> isValidOutputDirectory(project, f)).collect(Collectors.toList());
	}

	/**
	 * Moves all invalid output directories to the corrupted directory
	 */
	public static void cleanOutputDirs(final VadereProject project) {
		listAllDirs(project).stream().filter(f -> !isValidOutputDirectory(project, f))
				.forEach(dir -> cleanDirectory(project, dir));
	}

	public static Table readTrajectories(final VadereProject project, final String directoryName) throws IOException {
		TrajectoryReader reader = new TrajectoryReader(getPathToOutputFile(project, directoryName, IOUtils.TRAJECTORY_FILE_EXTENSION));
		return reader.readFile();
	}

	public static Table readTrajectories(final Path trajectoryFilePath) throws IOException {
		TrajectoryReader reader = new TrajectoryReader(trajectoryFilePath);
		Table result = reader.readFile();
		return result;
	}
	public static Table readContactData(final Path contactDataFilePath) throws IOException {
		ContactDataReader reader = new ContactDataReader(contactDataFilePath);
		Table result = reader.readFile();
		return result;
	}
	public static Table readAerosolCloudData(final Path aerosolCloudDataFilePath) throws IOException {
		AerosolCloudDataReader reader = new AerosolCloudDataReader(aerosolCloudDataFilePath);
		Table result = reader.readFile();
		return result;
	}

	/**
	 * Check if the trajectory file of the project is valid by only reading the first line of the file.
	 */
	private static boolean testTrajectories (final VadereProject project, final File directory) {
		try {
			TrajectoryReader reader = new TrajectoryReader(getPathToOutputFile(project, directory.getName(), IOUtils.TRAJECTORY_FILE_EXTENSION));
			reader.readFile();
			return true;

		} catch (IOException | VadereClassNotFoundException e) {
			logger.error("Error in output file " + directory.getName());
			return false;
		}
	}

    private static Optional<Table> readTrajectories(final VadereProject project, final File directory) {
        try {
            TrajectoryReader reader = new TrajectoryReader(getPathToOutputFile(project, directory.getName(), IOUtils.TRAJECTORY_FILE_EXTENSION));
            return Optional.of(reader.readFile());
        } catch (IOException | VadereClassNotFoundException e) {
            logger.error("Error in output file " + directory.getName());
            return Optional.empty();
        }
    }

	public static Scenario readScenarioRunManager(final VadereProject project, final String directoryName)
			throws IOException {
		String snapshotString = IOUtils
				.readTextFile(getPathToOutputFile(project, directoryName, IOUtils.SCENARIO_FILE_EXTENSION).toString());
		return IOVadere.fromJson(snapshotString);
	}

	public static Scenario readScenario(final File file) throws IOException {
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

	public static Scenario readScenario(final Path path) throws IOException {
		return IOOutput.readScenario(path.toFile());
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
					logger.error("could not deleteEdge scenario-file: " + file.getAbsolutePath() + ", "
							+ e.getLocalizedMessage());
				}
			}

			for (File file : trajectoryFile) {
				try {
					Files.delete(file.toPath());
				} catch (IOException e) {
					logger.error("could not deleteEdge trajectory-file: " + file.getAbsolutePath() + ", "
							+ e.getLocalizedMessage());
				}
			}

			try {
				Files.delete(directory.toPath());
			} catch (IOException e) {
				logger.error("could not delete output directory: " + directory.getAbsolutePath() + ", "
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

	private static void cleanDirectory(final VadereProject project, final File directory, boolean withGui){
		final String info = "The directory '"
				+ directory.getName()
				+ "' is corrupted and was moved to the '" + IOUtils.CORRUPT_DIR + "' folder.";

		if(withGui)
			IOUtils.errorBox(info, "Corrupt output file detected.");

		try {
			Files.createDirectories(Paths.get(project.getOutputDir().toString(), IOUtils.CORRUPT_DIR));
			Path sourcePath = directory.toPath();
			Path targetPath = Paths.get(project.getOutputDir().toString(), IOUtils.CORRUPT_DIR, directory.getName());
			Files.move(sourcePath, targetPath, StandardCopyOption.ATOMIC_MOVE);
			logger.info(info);
		} catch (IOException e1) {
			logger.error(e1);
		}
	}


	private static void cleanDirectory(final VadereProject project, final File directory) {
		cleanDirectory(project, directory, true);
	}

	/**
	 * Returns {@link SimulationOutput} if supplied directory is a valid output directory.
	 * @param project     VadereProject
	 * @param directory   Directory containing a simulated data
	 * @return            SimulationOutput contained in selected directory
	 */
	public static Optional<SimulationOutput> getSimulationOutput(final VadereProject project, final File directory ){
		if(!directory.exists())
			return Optional.empty();

		Optional<Scenario> scenario = readOutputFile(project, directory);
		if (scenario.isPresent() && testTrajectories(project, directory)){
			return Optional.of(new SimulationOutput(directory.toPath(), scenario.get()));
		} else {
			//if directory is not a valid OutputDirectory
			cleanDirectory(project, directory, false);
			return Optional.empty();
		}
	}

	/**
	 * Returns valid {@link SimulationOutput} of {@link VadereProject}
	 * @param project   VadereProject
	 * @return          All valid {@link SimulationOutput}s found in selected project.
	 */
	public static ConcurrentMap<String, SimulationOutput> getSimulationOutputs(final VadereProject project){
		List<File> simOutDir = IOOutput.listAllDirs(project);
		ConcurrentMap<String, SimulationOutput> simulationOutputs = new ConcurrentHashMap<>();
		simOutDir.forEach( f -> {
			Optional<Scenario> scenario = readOutputFile(project, f);
			if (scenario.isPresent() && testTrajectories(project, f)){
				SimulationOutput out = new SimulationOutput(f.toPath(), scenario.get());
				simulationOutputs.put(f.getName(), out);
			} else {
				//invalid output directory move to corrupt.
				cleanDirectory(project, f, false);
			}
		});
		return simulationOutputs;
	}

	private static Optional<Scenario> readOutputFile(final VadereProject project, final File directory) {
		try {
			final Path pathToSnapshot = getPathToOutputFile(project, directory.getName(), IOUtils.SCENARIO_FILE_EXTENSION);
			return Optional.of(IOVadere.fromJson(IOUtils.readTextFile(pathToSnapshot.toString())));
		} catch (IOException | VadereClassNotFoundException | IllegalArgumentException e ) {
			logger.error("Error in output file " + directory.getName());
			return Optional.empty();
		}
	}

	private static boolean isValidOutputDirectory(final VadereProject project, final File directory) {
		return readOutputFile(project, directory).isPresent() && testTrajectories(project, directory);
	}

	private static boolean isMatchingOutputDirectory(final VadereProject project, final File directory,
			final Scenario scenario) {
		Optional<Scenario> optionalScenario = readOutputFile(project, directory);
		return directory.isDirectory() && optionalScenario.isPresent() && equalHash(optionalScenario.get(), scenario);
	}

	private static boolean equalHash(final Scenario scenario1, Scenario scenario2) {
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
