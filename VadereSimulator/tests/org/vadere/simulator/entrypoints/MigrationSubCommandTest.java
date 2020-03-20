package org.vadere.simulator.entrypoints;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.entrypoints.cmd.SubCommand;
import org.vadere.simulator.entrypoints.cmd.VadereConsole;
import org.vadere.simulator.projects.migration.MigrationAssistant;
import org.vadere.simulator.utils.reflection.TestJsonNodeExplorer;
import org.vadere.simulator.utils.reflection.TestJsonNodeHelper;
import org.vadere.simulator.utils.reflection.TestResourceHandlerScenario;
import org.vadere.util.version.Version;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MigrationSubCommandTest implements TestJsonNodeExplorer, TestJsonNodeHelper, TestResourceHandlerScenario {

	private Path scenario1;
	private JsonNode scenario1Json;
	private Path scenario2;
	private JsonNode scenario2Json;

	private Path rootIgnore;
	private Path[] ignore = new Path[4];


	@Override
	public Path getTestDir() {
		return getPathFromResources("/migration/VadererConsole");
	}

	@Before
	public void init() {
		backupTestDir();

		scenario1 = getRelativeTestPath("v0.1_to_LATEST_Test1.scenario");
		scenario1Json = getJsonFromPath(scenario1);

		scenario2 = getRelativeTestPath("v0.1_to_LATEST_Test2.scenario");
		scenario2Json = getJsonFromPath(scenario2);

		rootIgnore = getRelativeTestPath("testDoNotMigrate");
		ignore[0] = rootIgnore.resolve("1").resolve("1.scenario");
		ignore[1] = rootIgnore.resolve("1/2").resolve("2.scenario");
		ignore[2] = rootIgnore.resolve("1/2/3").resolve("3.scenario");
		ignore[3] = rootIgnore.resolve("1/2/3/4").resolve("4.scenario");
	}

	@After
	public void clenaup() {
		loadFromBackup();
	}

	/**
	 * Test if the supplied file will be migrated to the latest version and if the legacy file is
	 * create correctly.
	 */
	@Test
	public void testMigrateAndRevertSingleFile() {

		assertReleaseVersion(scenario1Json, Version.V0_1, "Old Version must be 0.1");

		String[] args = new String[]{SubCommand.MIGRATE.getCmdName(), scenario1.toString()};
		VadereConsole.main(args);
		Path legacyFile = MigrationAssistant.getBackupPath(scenario1);
		assertTrue("There must be legacyFile", legacyFile.toFile().exists());

		scenario1Json = getJsonFromPath(scenario1);
		assertLatestReleaseVersion(scenario1Json);


		args = new String[]{SubCommand.MIGRATE.getCmdName(), "--revert-migration", scenario1.toString()};
		VadereConsole.main(args);

		scenario1Json = getJsonFromPath(scenario1);
		assertReleaseVersion(scenario1Json, Version.V0_1, "After revert the version must be 0.1");
		assertFalse("legacy file should be deleted after revert", legacyFile.toFile().exists());
	}

	@Test
	public void testMigrateAndRevertListOfFiles() {

		assertReleaseVersion(scenario1Json, Version.V0_1, "Old Version must be 0.1");
		assertReleaseVersion(scenario2Json, Version.V0_1, "Old Version must be 0.1");

		String[] args = new String[]{SubCommand.MIGRATE.getCmdName(), scenario1.toString(), scenario2.toString()};
		VadereConsole.main(args);

		scenario1Json = getJsonFromPath(scenario1);
		scenario2Json = getJsonFromPath(scenario2);
		assertLatestReleaseVersion(scenario1Json);
		assertLatestReleaseVersion(scenario2Json);


		Path legacyFile1 = MigrationAssistant.getBackupPath(scenario1);
		Path legacyFile2 = MigrationAssistant.getBackupPath(scenario2);
		assertTrue("There must be legacyFile1", legacyFile1.toFile().exists());
		assertTrue("There must be legacyFile2", legacyFile2.toFile().exists());


		args = new String[]{SubCommand.MIGRATE.getCmdName(), "--revert-migration", scenario1.toString(), scenario2.toString()};
		VadereConsole.main(args);

		scenario1Json = getJsonFromPath(scenario1);
		scenario2Json = getJsonFromPath(scenario2);
		assertReleaseVersion(scenario1Json, Version.V0_1, "After revert Version must be 0.1");
		assertReleaseVersion(scenario2Json, Version.V0_1, "After revert Version must be 0.1");

		assertFalse("File 1: legacy file should be deleted after revert", legacyFile1.toFile().exists());
		assertFalse("File 2: legacy file should be deleted after revert", legacyFile2.toFile().exists());

	}


	@Test
	public void testMigrationSameVersion() {

		assertReleaseVersion(scenario1Json, Version.V0_1, "Old Version must be 0.1");
		String[] args = new String[]{SubCommand.MIGRATE.getCmdName(), "--target-version", Version.V0_1.label(), scenario1.toString()};

		VadereConsole.main(args);
		Path legacyFile = MigrationAssistant.getBackupPath(scenario1);
		scenario1Json = getJsonFromPath(scenario1);
		assertReleaseVersion(scenario1Json, Version.V0_1, "NewVersion Version must be 0.1");

		assertFalse("No Transformation performed thus the should not be a legacyFile", legacyFile.toFile().exists());
	}

	/**
	 * Migrate a directory recursively
	 */
	@Test
	public void testIgnoreDirectoryAndDirectoryTreesRecursive() {
		for (Path path : ignore) {
			assertReleaseVersion(getJsonFromPath(path), Version.V0_1, "Old Version must be 0.1");
		}
		String[] args = new String[]{SubCommand.MIGRATE.getCmdName(), "-r", rootIgnore.toString()};
		VadereConsole.main(args);
		//only the second
		Version[] versions = new Version[]{Version.V0_1, Version.latest(), Version.V0_1, Version.V0_1};
		Boolean[] hasLegacyFile = new Boolean[]{false, true, false, false};
		for (int i = 0; i < ignore.length; i++) {
			assertReleaseVersion(
					getJsonFromPath(ignore[i]),
					versions[i],
					"(" + String.valueOf(i + 1) + ") Version of file not as accepted");
			Path legacy = MigrationAssistant.getBackupPath(ignore[i]);
			assertEquals("(" + String.valueOf(i + 1) + ") Existence of Backup File not as accented", legacy.toFile().exists(), hasLegacyFile[i]);
		}

	}

	@Test
	public void testIgnoreDirectoryAndDirectoryTreesNoneRecursive() {
		for (Path path : ignore) {
			assertReleaseVersion(getJsonFromPath(path), Version.V0_1, "Old Version must be 0.1");
		}
		String[] args = new String[]{SubCommand.MIGRATE.getCmdName(), rootIgnore.toString()};
		VadereConsole.main(args);
		for (Path path : ignore) {
			assertReleaseVersion(getJsonFromPath(path), Version.V0_1, "There should not be a new version");
			Path legacy = MigrationAssistant.getBackupPath(path);
			assertFalse(Files.exists(legacy));
		}

	}
}

