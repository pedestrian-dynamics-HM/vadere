package org.vadere.simulator.projects.migration;

import org.vadere.simulator.entrypoints.Version;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;

public abstract class MigrationAssistant {
	public static final String INCIDENT_ORDER_ERROR = "An incident that was found applicable couldn't be resolved. " +
			"That means, that a previously resolved incident rendered this one no longer applicable. " +
			"Check the order of the incidents in the IncidentDatabase for logical flaws.";
	protected final MigrationOptions migrationOptions;

	public MigrationAssistant(MigrationOptions migrationOptions) {
		this.migrationOptions = migrationOptions;
	}

	public static MigrationAssistant getNewInstance(MigrationOptions options) {
		if (options.isUseDeprecatedAssistant()) {
			return new IncidentMigrationAssistant(options);
		} else {
			return new JoltMigrationAssistant(options);
		}
	}

	public static Path getBackupPath(Path scenarioFile){
		return IOUtils.addSuffix(scenarioFile, "." + IOUtils.LEGACY_DIR, false);
	}

	public abstract String getLog();

	public abstract void restLog();


	public abstract MigrationResult analyzeProject(String projectFolderPath) throws IOException;

	protected String getTimestamp() {
		return new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
	}

	public abstract String convertFile(Path scenarioFilePath, Version targetVersion) throws MigrationException;


	public abstract void migrateFile(Path scenarioFilePath, Version targetVersion,  Path outputFile) throws MigrationException;
	public abstract void revertFile(Path scenarioFile) throws MigrationException;
}
