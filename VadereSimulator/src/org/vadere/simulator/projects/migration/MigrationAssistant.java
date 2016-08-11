package org.vadere.simulator.projects.migration;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.simulator.projects.migration.incidents.ExceptionIncident;
import org.vadere.simulator.projects.migration.incidents.Incident;
import org.vadere.simulator.projects.migration.incidents.VersionBumpIncident;
import org.vadere.util.io.IOUtils;

public class MigrationAssistant {

	private static final List<String> versions = Arrays.asList("not a release", "0.1"); // put somewhere more central?
	public static final String latestVersion = versions.get(versions.size() - 1);

	public static final String INCIDENT_ORDER_ERROR = "An incident that was found applicable couldn't be resolved. " +
			"That means, that a previously resolved incident rendered this one no longer applicable. " +
			"Check the order of the incidents in the IncidentDatabase for logical flaws.";

	private static final String LEGACY_DIR = "legacy";
	private static final String LEGACY_EXTENSION = "legacy";
	private static final String NONMIGRATABLE_EXTENSION = "nonmigratable";

	private static Logger logger = LogManager.getLogger(MigrationAssistant.class);

	public static void analyzeSingleScenario(Path path) {
		// TODO [priority=high] [task=implement] for runs initiated not from GUI... where to hook in?
	}

	public static int[] analyzeProject(String projectFolderPath) throws IOException {

		int[] stats = {0, 0, 0};

		Path scenarioDir = Paths.get(projectFolderPath, IOUtils.SCENARIO_DIR);
		if (Files.exists(scenarioDir)) {
			stats = analyzeDirectory(scenarioDir, true);
		}

		Path outputDir = Paths.get(projectFolderPath, IOUtils.OUTPUT_DIR);
		if (Files.exists(outputDir)) {
			int[] outputDirStats = analyzeDirectory(outputDir, false);
			for (int i = 0; i < outputDirStats.length; i ++) {
				stats[i] += outputDirStats[i];
			}
		}

		return stats;
	}

	// if isScenario is false, its output
	private static int[] analyzeDirectory(Path dir, boolean isScenario) throws IOException {
		StringBuilder log = new StringBuilder();

		Path legacyDir = null;
		if (isScenario) {
			legacyDir = dir.getParent().resolve(LEGACY_DIR).resolve("scenarios");
		}

		int[] stats = new int[3]; // scenarios: [0] total, [1] legacy'ed, [2] nonmigratable

		File[] scenarioFiles = isScenario ? IOUtils.getFilesInScenarioDirectory(dir) : IOUtils.getScenarioFilesInOutputDirectory(dir);
		stats[0] = scenarioFiles.length;

		int legacyedCount = 0;
		int nonmigratableCount = 0;
		for (File file : scenarioFiles) {

			if (!isScenario) {
				String fileFolder = Paths.get(file.getParent()).getFileName().toString(); // the folder in which the .scenario and the .trajectories file lies
				legacyDir = dir.getParent().resolve(LEGACY_DIR).resolve("output").resolve(fileFolder);
			}

			Path scenarioFilePath = Paths.get(file.getAbsolutePath());
			try {
				if (analyzeScenario(scenarioFilePath, legacyDir, log, isScenario)) {
					legacyedCount++;
				}
			} catch (MigrationException e) {
				moveFileAddExtension(scenarioFilePath, legacyDir, NONMIGRATABLE_EXTENSION);
				log.append(
						"! --> Can't migrate the scenario to latest version, removed it from the directory ("
								+ e.getMessage() + ")\n" +
								"If you can fix this problem manually, do so and then remove ."
								+ NONMIGRATABLE_EXTENSION + " from the file in the " + LEGACY_DIR + "-directory " +
								"and move it back into the scenarios-directory, it will be checked again when the GUI restarts.\n");
				nonmigratableCount++;
			}
		}

		if (!isScenario) {
			legacyDir = dir.getParent().resolve(LEGACY_DIR).resolve("output");
		}

		if (legacyedCount + nonmigratableCount > 0)
			IOUtils.writeTextFile(legacyDir.resolve("_LOG-" + getTimestamp() + ".txt").toString(), log.toString());

		stats[1] = legacyedCount;
		stats[2] = nonmigratableCount;
		return stats;
	}

	private static boolean analyzeScenario(Path scenarioFilePath, Path legacyDir, StringBuilder log, boolean isScenario) throws IOException, MigrationException {
		String json = IOUtils.readTextFile(scenarioFilePath);
		JsonNode node = JsonConverter.deserializeToNode(json);
		Graph graph = new Graph(node);

		String outputScenarioParentFolderName = isScenario ? "" : scenarioFilePath.getParent().getFileName().toString() + " _ ";

		log.append("\n>> analyzing JSON tree of scenario <" + outputScenarioParentFolderName + node.get("name").asText() + ">\n");

		String releaseVersion = "undefined";
		int releaseVersionIndex = 0;

		if (node.get("release") != null) {
			releaseVersion = node.get("release").asText();
			if (releaseVersion.equals(latestVersion))
				return false;

			releaseVersionIndex = versions.indexOf(releaseVersion);
			if (releaseVersionIndex == -1)
				throw new MigrationException("release version " + releaseVersion
						+ " is unknown - if this is actually a valid release, update the version-list in MigrationAssistant accordingly");
		}

		// 1. collect possible incidents

		List<Incident> possibleIncidents = new ArrayList<>();
		for (int version = releaseVersionIndex; version < versions.size() - 1; version++) {
			log.append("  > checking possible incidents from version \"" + versions.get(version) + "\" to version \""
					+ versions.get(version + 1) + "\"\n");
			possibleIncidents.addAll(IncidentDatabase.getInstance().getPossibleIncidentsFor(version));
		}
		possibleIncidents.add(new ExceptionIncident(node));
		possibleIncidents.add(new VersionBumpIncident(node, releaseVersion));

		// 2. filter those out that don't apply

		List<Incident> applicableIncidents = possibleIncidents.stream()
				.filter(incident -> incident.applies(graph))
				.collect(Collectors.toList());

		// 3. resolve the applicable incidents (step 2 and 3 are intentionally separated to uncover
		//		potentially dangerous flaws in the order of the incidents in the IncidentDatabase)

		for (Incident incident : applicableIncidents)
			incident.resolve(graph, log);

		if (legacyDir != null) {
			moveFileAddExtension(scenarioFilePath, legacyDir, LEGACY_EXTENSION);
		}
		IOUtils.writeTextFile(scenarioFilePath.toString(), JsonConverter.serializeJsonNode(node));
		return true;
	}

	private static void moveFileAddExtension(Path scenarioFilePath, Path legacyDir, String additionalExtension)
			throws IOException {
		IOUtils.createDirectoryIfNotExisting(legacyDir);
		Files.move(scenarioFilePath, legacyDir.resolve(scenarioFilePath.getFileName() + "." + additionalExtension),
				StandardCopyOption.REPLACE_EXISTING); // ensure potential existing files aren't overwritten?
	}

	private static String getTimestamp() {
		return new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
	}

}
