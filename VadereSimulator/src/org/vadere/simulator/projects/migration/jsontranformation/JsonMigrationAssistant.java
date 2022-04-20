package org.vadere.simulator.projects.migration.jsontranformation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationAssistant;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.MigrationLogger;
import org.vadere.simulator.projects.migration.MigrationOptions;
import org.vadere.simulator.projects.migration.MigrationResult;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static org.vadere.util.io.IOUtils.LEGACY_DIR;
import static org.vadere.util.io.IOUtils.OUTPUT_DIR;
import static org.vadere.util.io.IOUtils.SCENARIO_DIR;


public class JsonMigrationAssistant extends MigrationAssistant {

	private final static Logger logger = Logger.getLogger(JsonMigrationAssistant.class);
	private MigrationLogger migrationLogger;


	public JsonMigrationAssistant(MigrationOptions options) {
		super(options);
		migrationLogger = new MigrationLogger();
	}

	public JsonMigrationAssistant() {
		this(MigrationOptions.defaultOptions());
	}

	@Override
	public String getLog() {
		return migrationLogger.getLog();
	}

	@Override
	public void restLog() {
		migrationLogger.rest();
	}


	@Override
	public MigrationResult analyzeProject(String projectFolderPath) throws IOException {
		MigrationResult stats = new MigrationResult();

		Path scenarioDir = Paths.get(projectFolderPath, SCENARIO_DIR);
		if (Files.exists(scenarioDir)) {
			stats = migrateDirectory(scenarioDir, SCENARIO_DIR);
		}

		Path outputDir = Paths.get(projectFolderPath, OUTPUT_DIR);
		if (Files.exists(outputDir)) {
			MigrationResult outputDirStats = migrateDirectory(outputDir, OUTPUT_DIR);
			stats.add(outputDirStats);
		}
		return stats;
	}

	public String migrateScenarioFile(String json, Version targetVersion) throws MigrationException {

		JsonNode node;
		try {
			node = StateJsonConverter.deserializeToNode(json);
		} catch (IOException e) {
			logger.error("Error converting File: " + e.getMessage());
			throw new MigrationException("Could not create Json representation" + e.getMessage());
		}
		restLog();
		migrationLogger.info(">> analyzing JSON tree of scenario <" + node.get("name").asText() + ">");
		logger.info(migrationLogger.last());
		Version version;
		if (node.get("release") != null) {
			version = Version.fromString(node.get("release").asText());

			if (version == null || version.equalOrSmaller(Version.UNDEFINED)) {
				migrationLogger.error("release version " + node.get("release").asText() + " is unknown or not " +
						"supported. If this is a valid release create a version transformation and a new idenity transformation");
				logger.error(migrationLogger.last());
				throw new MigrationException("release version " + node.get("release").asText() + " is unknown or not " +
						"supported. If this is a valid releasecreate a version transformation and a new idenity transformation");
			}
		} else {
			migrationLogger.warn("Version is unknown of scenario <" + node.get("name").asText() + ">! Try to use " + Version.NOT_A_RELEASE.label() + " as Version for transformation.");
			logger.warn(migrationLogger.last());
			version = Version.NOT_A_RELEASE;
		}

		if (version.equals(targetVersion)) {
			migrationLogger.info("Nothing to do current version and target version match");
			logger.info(migrationLogger.last());
			restLog();
			return null;
		}

		JsonNode transformedNode = node;
		// apply all transformation from current to latest version.
		for (Version v : Version.listVersionFromTo(version, targetVersion)) {
			migrationLogger.info("<" + node.get("name").asText() + "> Start Transform to Version: " + v.label());
			logger.info(migrationLogger.last());
			transformedNode = transform(transformedNode, v);
		}
		if (targetVersion.equals(Version.latest())){
			transformedNode = AbstractJsonTransformation.addNewMembersWithDefaultValues(transformedNode);
		}

		try {
			restLog();
			return StateJsonConverter.serializeJsonNode(transformedNode);
		} catch (JsonProcessingException e) {
			logger.error("could not serializeJsonNode after Transformation: " + e.getMessage());
			throw new MigrationException("could not serializeJsonNode after Transformation: " + e.getMessage());
		}

	}

	// will return null if current and target version match
	@Override
	public String migrateScenarioFile(Path scenarioFilePath, Version targetVersion) throws MigrationException {
		String json = null;
		try {
			json = IOUtils.readTextFile(scenarioFilePath);
		} catch (IOException e) {
			logger.error("Error converting File: " + e.getMessage());
			throw new MigrationException("Could not read JsonFile." + e.getMessage());
		}

		return migrateScenarioFile(json, targetVersion);
	}


	@Override
	public void migrateScenarioFile(Path scenarioFilePath, Version targetVersion, Path outputFile) throws MigrationException {
		String json = migrateScenarioFile(scenarioFilePath, targetVersion);
		if (json == null) {
			logger.info("Nothing todo scenarioFile up-to-date");
			return;
		}

		if (outputFile == null || scenarioFilePath.equals(outputFile)) {
			//overwrite scenarioFile
			Path backupPath = getBackupPath(scenarioFilePath);
			try {
				Files.copy(scenarioFilePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
				IOUtils.writeTextFile(scenarioFilePath.toString(), json);
			} catch (IOException e) {
				logger.error("Cannot overwrite scenarioFile or cannot write new file new version: " + e.getMessage(), e);
				throw new MigrationException("Cannot overwrite scenarioFile or cannot write new file new version: " + e.getMessage(), e);
			}
		} else {
			try {
				IOUtils.writeTextFile(outputFile.toString(), json);
			} catch (IOException e) {
				throw new MigrationException("Cannot write to output file:  " + e.getMessage(), e);
			}
		}
	}


	@Override
	public void revertFile(Path scenarioFile) throws MigrationException {
		Path backupFile = MigrationAssistant.getBackupPath(scenarioFile);
		if (!backupFile.toFile().exists()) {
			logger.error("There does not exist a Backup for the given file");
			logger.error("File: " + scenarioFile.toString());
			logger.error("Backup does not exist: " + backupFile.toString());
		}

		try {
			Files.copy(backupFile, scenarioFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			logger.error("Could not copy legacy backup to current version: " + e.getMessage(), e);
			throw new MigrationException("Could not copy legacy backup to current version: " + e.getMessage(), e);
		}
		try {
			Files.deleteIfExists(backupFile);
		} catch (IOException e) {
			logger.error("Cold not delete old legacy file after reverting File: " + e.getMessage(), e);
		}
	}


	private MigrationResult migrateDirectory(Path dir, String dirName) throws IOException {

		Path legacyDir = dir.getParent().resolve(LEGACY_DIR).resolve(dirName);

		File[] scenarioFiles = dirName.equals(SCENARIO_DIR) ? IOUtils.getFilesInScenarioDirectory(dir) : IOUtils.getScenarioFilesInOutputDirectory(dir);
		MigrationResult stats = new MigrationResult(scenarioFiles.length);

		for (File file : scenarioFiles) {

			if (dirName.equals(OUTPUT_DIR)) {
				String fileFolder = Paths.get(file.getParent()).getFileName().toString(); // the folder in which the .scenario and the .trajectories file lies
				legacyDir = dir.getParent().resolve(LEGACY_DIR).resolve(OUTPUT_DIR).resolve(fileFolder);
			}

			// Ignore meshes
			if(file.isDirectory() && file.getName().equals(IOUtils.MESH_DIR)) {
				continue;
			}

			Path scenarioFilePath = Paths.get(file.getAbsolutePath());
			try {
				if (migrateScenario(scenarioFilePath, legacyDir, dirName)) {
					stats.legacy++;
				} else {
					stats.upToDate++;
				}
			} catch (MigrationException e) {
				moveFileAddExtension(scenarioFilePath, legacyDir, migrationOptions.getNonmigratabelExtension(), !dirName.equals(SCENARIO_DIR));
				migrationLogger.error("!> Can't migrate the scenario to latest version, removed it from the directory (" +
						e.getMessage() + ") If you can fix this problem manually, do so and then remove ." +
						migrationOptions.getNonmigratabelExtension() + " from the file in the " + LEGACY_DIR + "-directory "
						+ "and move it back into the scenarios-directory, it will be checked again when the GUI restarts.");
				logger.error(migrationLogger.last());
				stats.notmigratable++;
			}
		}

		if (stats.legacy + stats.notmigratable > 0)
			migrationLogger.writeLog(legacyDir.resolve("_LOG-" + getTimestamp() + ".txt").toString());

		// clean appender for next run with same JoltMigrationAssistant instance
		restLog();
		return stats;
	}

	public JsonNode transform(JsonNode currentJson, Version targetVersion) throws MigrationException {
		JsonTransformation t = AbstractJsonTransformation.get(targetVersion.previousVersion());
		currentJson = t.applyPreHooks(currentJson);
		currentJson = t.applyTransformation(currentJson);
		currentJson = t.applyPostHooks(currentJson);
		return currentJson;
	}

	private boolean migrateScenario(Path scenarioFilePath, Path legacyDir, String dirName)
			throws IOException, MigrationException {
		String json = IOUtils.readTextFile(scenarioFilePath);
		JsonNode node = StateJsonConverter.deserializeToNode(json);

		String parentPath = dirName.equals(SCENARIO_DIR) ? SCENARIO_DIR + "/" : OUTPUT_DIR + "/" + scenarioFilePath.getParent().getFileName().toString() + "/";

		migrationLogger.info("Analyzing scenario file " + parentPath + node.get("name").asText());
		logger.debug(migrationLogger.last());

		Version version;

		if (node.get("release") != null) {
			version = Version.fromString(node.get("release").asText());

			if (version == null || version.equalOrSmaller(Version.UNDEFINED)) {
				migrationLogger.error("release version " + node.get("release").asText() + " is unknown or not " +
						"supported. If this is a valid release create a version transformation and a new idenity transformation");
				logger.error(migrationLogger.last());
				throw new MigrationException("release version " + node.get("release").asText() + " is unknown or not " +
						"supported. If this is a valid releasecreate a version transformation and a new idenity transformation");
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
		} else {
			migrationLogger.warn("Version is unknown of scenario <" + parentPath + node.get("name").asText() + ">! Try to use " + Version.NOT_A_RELEASE.label() + " as Version for transformation.");
			logger.warn(migrationLogger.last());
			version = Version.NOT_A_RELEASE;
		}

		JsonNode transformedNode = node;
		// apply all transformation from current to latest version.
		for (Version v : Version.listToLatest(version)) {
			migrationLogger.info("<" + node.get("name").asText() + "> Transform to: " + v.label());
			logger.debug(migrationLogger.last());
			transformedNode = transform(transformedNode, v);
		}
		// will always be Version.latest()
		transformedNode = AbstractJsonTransformation.addNewMembersWithDefaultValues(transformedNode);
		if (legacyDir != null) {
			migrationLogger.info("Migration successful. Move copy of old version to \"legacy\" directory");
			logger.debug(migrationLogger.last());
			moveFileAddExtension(scenarioFilePath, legacyDir, migrationOptions.getLegacyExtension(), false);
		}
		IOUtils.writeTextFile(scenarioFilePath.toString(), StateJsonConverter.serializeJsonNode(transformedNode));
		return true;
	}

	private void moveFileAddExtension(Path scenarioFilePath, Path legacyDir, String additionalExtension, boolean moveOutputFolder)
			throws IOException {
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
