package org.vadere.simulator.projects.migration.incident;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationAssistant;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.MigrationOptions;
import org.vadere.simulator.projects.migration.MigrationResult;
import org.vadere.simulator.projects.migration.incident.incidents.ExceptionIncident;
import org.vadere.simulator.projects.migration.incident.incidents.Incident;
import org.vadere.simulator.projects.migration.incident.incidents.VersionBumpIncident;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class IncidentMigrationAssistant extends MigrationAssistant {

	private static Logger logger = Logger.getLogger(IncidentMigrationAssistant.class);
	StringBuilder log;

	public IncidentMigrationAssistant() {
		super(MigrationOptions.defaultOptions());
		StringBuilder log = new StringBuilder();
	}

	public IncidentMigrationAssistant(final MigrationOptions migrationOptions) {
		super(migrationOptions);
		log = new StringBuilder();
	}

	@Override
	public String getLog() {
		return log.toString();
	}

	@Override
	public void restLog() {
		log.setLength(0);
	}


//

	@Override
	public MigrationResult analyzeProject(String projectFolderPath) throws IOException {

		MigrationResult stats = new MigrationResult();

		Path scenarioDir = Paths.get(projectFolderPath, IOUtils.SCENARIO_DIR);
		if (Files.exists(scenarioDir)) {
			stats = analyzeDirectory(scenarioDir, true);
		}

		Path outputDir = Paths.get(projectFolderPath, IOUtils.OUTPUT_DIR);
		if (Files.exists(outputDir)) {
			MigrationResult outputDirStats = analyzeDirectory(outputDir, false);
			stats.add(outputDirStats);
		}


		return stats;
	}

	@Override
	public String migrateScenarioFile(Path scenarioFilePath, Version targetVersion) {
		throw new RuntimeException("Not Implemented");
	}

	@Override
	public void migrateScenarioFile(Path scenarioFilePath, Version targetVersion, Path outputFile) {
		throw new RuntimeException("Not Implemented");
	}


	@Override
	public void revertFile(Path scenarioFile) {
		throw new RuntimeException("Not Implemented");
	}

	// if isScenario is false, its output
	private MigrationResult analyzeDirectory(Path dir, boolean isScenario) throws IOException {

		Path legacyDir = null;
		if (isScenario) {
			legacyDir = dir.getParent().resolve(IOUtils.LEGACY_DIR).resolve("scenarios");
		}


		File[] scenarioFiles = isScenario ? IOUtils.getFilesInScenarioDirectory(dir) : IOUtils.getScenarioFilesInOutputDirectory(dir);
		MigrationResult stats = new MigrationResult(scenarioFiles.length); // scenarios: [0] total, [1] legacy'ed, [2] nonmigratable

		int legacyedCount = 0;
		int nonmigratableCount = 0;
		for (File file : scenarioFiles) {

			if (!isScenario) {
				String fileFolder = Paths.get(file.getParent()).getFileName().toString(); // the folder in which the .scenario and the .trajectories file lies
				legacyDir = dir.getParent().resolve(IOUtils.LEGACY_DIR).resolve("output").resolve(fileFolder);
			}

			Path scenarioFilePath = Paths.get(file.getAbsolutePath());
			try {
				if (analyzeScenario(scenarioFilePath, legacyDir, isScenario)) {
					stats.legacy++;
				} else {
					stats.upToDate++;
				}
			} catch (MigrationException e) {
				moveFileAddExtension(scenarioFilePath, legacyDir, migrationOptions.getNonmigratabelExtension(), !isScenario);
				log.append(
						"! --> Can't migrate the scenario to latest version, removed it from the directory ("
								+ e.getMessage() + ")\n" +
								"If you can fix this problem manually, do so and then remove ."
								+ migrationOptions.getNonmigratabelExtension() + " from the file in the " + IOUtils.LEGACY_DIR + "-directory " +
								"and move it back into the scenarios-directory, it will be checked again when the GUI restarts.\n");
				stats.notmigratable++;
			}
		}

		if (!isScenario) {
			legacyDir = dir.getParent().resolve(IOUtils.LEGACY_DIR).resolve("output");
		}

		if (stats.legacy + stats.notmigratable > 0)
			IOUtils.writeTextFile(legacyDir.resolve("_LOG-" + getTimestamp() + ".txt").toString(), log.toString());

		return stats;
	}

	private boolean analyzeScenario(Path scenarioFilePath, Path legacyDir, boolean isScenario) throws IOException, MigrationException {
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
			if (migrationOptions.isReapplyLatestMigrationFlag() && migrationOptions.getBaseVersion() != null) {
				version = migrationOptions.getBaseVersion();

			} else if (migrationOptions.isReapplyLatestMigrationFlag()) { // if enforced migration should be done from prev version to latest
				Optional<Version> optVersion = Version.getPrevious(version);
				if (optVersion.isPresent()) {
					version = optVersion.get();
				} else {
					return false;
				}
			} // if no enforced migration should be done and we are at the latest version, no migration is required.
			else if (version == Version.latest()) {
				return false;
			}
		}

		// 1. collect possible incidents

		List<Incident> possibleIncidents = new ArrayList<>();
		for (int versionIndex = version.ordinal(); versionIndex < Version.latest().ordinal(); versionIndex++) {
			Version ver = Version.values()[versionIndex];
			log.append("  > checking possible incidents from version \"")
					.append(ver.label()).append("\" to version \"")
					.append(Version.values()[versionIndex + 1].label()).append("\"\n");
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
			moveFileAddExtension(scenarioFilePath, legacyDir, migrationOptions.getLegacyExtension(), false);
		}
		IOUtils.writeTextFile(scenarioFilePath.toString(), StateJsonConverter.serializeJsonNode(node));
		return true;
	}

	private void moveFileAddExtension(Path scenarioFilePath, Path legacyDir, String additionalExtension, boolean moveOutputFolder) throws IOException {
		Path source = scenarioFilePath;
		Path target = legacyDir.resolve(source.getFileName() + "." + additionalExtension);

		if (moveOutputFolder) {
			source = source.getParent();
			target = Paths.get(legacyDir.toAbsolutePath() + "." + additionalExtension);
		}

		IOUtils.createDirectoryIfNotExisting(target);
		Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); // ensure potential existing files aren't overwritten?
	}

}
