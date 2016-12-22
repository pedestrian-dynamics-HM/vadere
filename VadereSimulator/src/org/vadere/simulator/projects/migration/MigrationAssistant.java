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
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.incidents.ExceptionIncident;
import org.vadere.simulator.projects.migration.incidents.Incident;
import org.vadere.simulator.projects.migration.incidents.VersionBumpIncident;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.io.IOUtils;

public class MigrationAssistant {

	public static final String INCIDENT_ORDER_ERROR = "An incident that was found applicable couldn't be resolved. " +
			"That means, that a previously resolved incident rendered this one no longer applicable. " +
			"Check the order of the incidents in the IncidentDatabase for logical flaws.";

	private static final String LEGACY_DIR = "legacy";
	private static final String LEGACY_EXTENSION = "legacy";
	private static final String NONMIGRATABLE_EXTENSION = "nonmigratable";

	private static Logger logger = LogManager.getLogger(MigrationAssistant.class);

	private static boolean reapplyLatestMigrationFlag = false;
	private static Version baseVersion = null;

	public static void setReapplyLatestMigrationFlag() {
		reapplyLatestMigrationFlag = true;
		baseVersion = null;
	}

	public static void setReapplyLatestMigrationFlag(final Version version) {
		reapplyLatestMigrationFlag = true;
		baseVersion = version;
	}

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

		reapplyLatestMigrationFlag = false;
		baseVersion = null;

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
				moveFileAddExtension(scenarioFilePath, legacyDir, NONMIGRATABLE_EXTENSION, !isScenario);
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
		JsonNode node = StateJsonConverter.deserializeToNode(json);
		Tree tree = new Tree(node);

		String outputScenarioParentFolderName = isScenario ? "" : scenarioFilePath.getParent().getFileName().toString() + " _ ";

		log.append("\n>> analyzing JSON tree of scenario <" + outputScenarioParentFolderName + node.get("name").asText() + ">\n");

		Version version = Version.UNDEFINED;

		if (node.get("release") != null) {
			version = Version.fromString(node.get("release").asText());

			if (version == null) {
				throw new MigrationException("release version " + node.get("release").asText() + " is unknown. If this " +
						"is a valid release, update the version-list in MigrationAssistant accordingly");
			}

			// if enforced migration should be done from baseVersion to latestVersion
			if (reapplyLatestMigrationFlag && baseVersion != null) {
				version = baseVersion;

			} else if(reapplyLatestMigrationFlag) { // if enforced migration should be done from prev version to latest
				Optional<Version> optVersion = Version.getPrevious(version);
				if(optVersion.isPresent()) {
					version = optVersion.get();
				}
				else {
					return false;
				}
			} // if no enforced migration should be done and we are at the latest version, no migration is required.
			else if(version == Version.latest()) {
				return false;
			}
		}

		// 1. collect possible incidents

		List<Incident> possibleIncidents = new ArrayList<>();
		for (int versionIndex = version.ordinal(); versionIndex < Version.latest().ordinal(); versionIndex ++) {
			Version ver = Version.values()[versionIndex];
			log.append("  > checking possible incidents from version \"" + ver.label() + "\" to version \""
					+ Version.values()[versionIndex + 1].label() + "\"\n");
			possibleIncidents.addAll(IncidentDatabase.getInstance().getPossibleIncidentsFor(ver));
		}
		possibleIncidents.add(new ExceptionIncident(node));
		possibleIncidents.add(new VersionBumpIncident(node, version));

		// 2. filter those out that don't apply

		List<Incident> applicableIncidents = possibleIncidents.stream()
				.filter(incident -> incident.applies(tree))
				.collect(Collectors.toList());

		// 3. resolve the applicable incidents (step 2 and 3 are intentionally separated to uncover
		//		potentially dangerous flaws in the order of the incidents in the IncidentDatabase)

		for (Incident incident : applicableIncidents)
			incident.resolve(tree, log);

		if (legacyDir != null) {
			moveFileAddExtension(scenarioFilePath, legacyDir, LEGACY_EXTENSION, false);
		}
		IOUtils.writeTextFile(scenarioFilePath.toString(), StateJsonConverter.serializeJsonNode(node));
		return true;
	}

	private static void moveFileAddExtension(Path scenarioFilePath, Path legacyDir, String additionalExtension, boolean moveOutputFolder) throws IOException {
		Path source = scenarioFilePath;
		Path target = legacyDir.resolve(source.getFileName() + "." + additionalExtension);

		if (moveOutputFolder) {
			source = source.getParent();
			target = Paths.get(legacyDir.toAbsolutePath() + "." + additionalExtension);
		}

		IOUtils.createDirectoryIfNotExisting(target);
		Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); // ensure potential existing files aren't overwritten?
	}

	private static String getTimestamp() {
		return new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
	}

}
