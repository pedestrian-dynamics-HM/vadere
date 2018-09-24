package org.vadere.simulator.entrypoints.cmd.commands;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import org.apache.log4j.Logger;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.entrypoints.cmd.SubCommandRunner;
import org.vadere.simulator.projects.migration.MigrationAssistant;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.MigrationOptions;
import org.vadere.simulator.projects.migration.helper.MigrationUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MigrationSubCommand implements SubCommandRunner {
	private final static Logger logger = Logger.getLogger(MigrationSubCommand.class);

	@Override
	public void run(Namespace ns, ArgumentParser parser) throws Exception {

		String outputPathString = ns.getString("output-file");
		Version targetVersion = Version.fromString(ns.getString("target-version"));
		boolean revertMode = ns.getBoolean("revert-migration");
		boolean recursive = ns.getBoolean("recursive");
		List<String> paths = ns.getList("paths");
		String newVersion = ns.getString("create-new-version");

		ArrayList<Path> files = new ArrayList<>();
		ArrayList<Path> dirs = new ArrayList<>();
		ArrayList<Path> err = new ArrayList<>();
		for (String path : paths) {
			Path tmp = Paths.get(path);
			if (tmp.toFile().exists()) {
				if (tmp.toFile().isFile()) {
					files.add(tmp);
				} else if (tmp.toFile().isDirectory()) {
					dirs.add(tmp);
				} else {
					err.add(tmp);
				}
			} else {
				err.add(tmp);
			}
		}

		if (err.size() > 0) {
			logger.error("some input input paths. Stop processing");
			err.stream().forEach(e -> logger.error("Path does not exist or missing permissions: " + e));
			throw new MigrationException("Error in input paths");
		}

		if (newVersion != null && dirs.size() == 1){
			createNewTransformFiles(dirs.get(0), newVersion);
			return;
		}

		if (revertMode) {
			for (Path file : files) {
				logger.info("revert file: " + file.toString());
				revert(file);
			}
			MigrationUtil migrationUtil = new MigrationUtil();
			for (Path dir : dirs) {
				logger.info("revert directory: " + dir.toString());
				migrationUtil.revertDirectoryTree(dir, recursive);
			}
		} else {
			for (Path file : files) {
				logger.info("migrate file version(" + targetVersion.label() + "): " + file.toAbsolutePath().toString());
				migrate(file, targetVersion, outputPathString);
			}
			MigrationUtil migrationUtil = new MigrationUtil();
			for (Path dir : dirs) {
				logger.info("migrate directory to version(" + targetVersion.label() + "): " + dir.toAbsolutePath().toString());
				migrationUtil.migrateDirectoryTree(dir, targetVersion, recursive);
			}
		}

	}

	private void createNewTransformFiles(Path dest, String versionLabel) throws MigrationException {
		MigrationUtil migrationUtil = new MigrationUtil();
		try {
			migrationUtil.generateNewVersionTransform(dest, versionLabel);
		} catch (URISyntaxException e) {
			throw new MigrationException("Error creating new transformation", e);
		} catch (IOException e) {
			throw new MigrationException("Error creating new transformation", e);
		}

	}

	private void revert(Path scenarioFile) throws MigrationException {
		MigrationAssistant ma = MigrationAssistant.getNewInstance(MigrationOptions.defaultOptions());
		ma.revertFile(scenarioFile);
	}

	private void migrate(Path scenarioFile, Version targetVersion, String outputPathString) {
		MigrationAssistant ma = MigrationAssistant.getNewInstance(MigrationOptions.defaultOptions());
		String out;
		try {
			logger.info("Scenario file" + scenarioFile.getFileName().toString());
			logger.info("Try to migrate to version " + targetVersion);
			Path outputFile = null;
			if (outputPathString != null) {
				if (!Paths.get(outputPathString).toFile().isDirectory()) {
					throw new MigrationException("output-file must be a directory:" + outputPathString);

				}
				outputFile = Paths.get(outputPathString).resolve(scenarioFile.getFileName());
			}

			ma.migrateFile(scenarioFile, targetVersion, outputFile);


		} catch (Exception e) {
			logger.error("Migration failed", e);
		}
	}

}
