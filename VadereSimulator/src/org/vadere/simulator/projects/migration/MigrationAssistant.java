package org.vadere.simulator.projects.migration;

import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.incident.IncidentMigrationAssistant;
import org.vadere.simulator.projects.migration.jsontranformation.JsonMigrationAssistant;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;

public abstract class MigrationAssistant {
	protected final MigrationOptions migrationOptions;

	public MigrationAssistant(MigrationOptions migrationOptions) {
		this.migrationOptions = migrationOptions;
	}

	public static MigrationAssistant getNewInstance(MigrationOptions options) {
		if (options.isUseDeprecatedAssistant()) {
			return new IncidentMigrationAssistant(options);
		} else {
			return new JsonMigrationAssistant(options);
		}
	}

	public static Path getBackupPath(Path scenarioFile){
		return IOUtils.addSuffix(scenarioFile, "." + IOUtils.LEGACY_DIR, false);
	}

	public abstract String getLog();

	public abstract void restLog();




	protected String getTimestamp() {
		return new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
	}

	public abstract MigrationResult analyzeProject(String projectFolderPath) throws IOException;

	/**
	 * Migrate a given serialized scenario file to the target version.
	 * @param scenarioFilePath		Path to scenario file
	 * @param targetVersion			Version number to which it should be upgraded.
	 * @return						String representation (JSON) of the serialized scenario
	 * @throws MigrationException
	 */
	public abstract String migrateScenarioFile(Path scenarioFilePath, Version targetVersion) throws MigrationException;


	/**
	 * Migrate a given serialized scenario file to the target version and save the result to a file.
	 * @param scenarioFilePath		Path to scenario file
	 * @param targetVersion			Version number to which it should be upgraded.
	 * @param outputFile			If Null overwrite existing file. Backup as {filename}.legacy
	 * @throws MigrationException
	 */
	public abstract void migrateScenarioFile(Path scenarioFilePath, Version targetVersion, Path outputFile) throws MigrationException;

	/**
	 * Search for existing backup files at default location.
	 * @param scenarioFile			Path to scenario file
	 * @throws MigrationException
	 */
	public abstract void revertFile(Path scenarioFile) throws MigrationException;
}
