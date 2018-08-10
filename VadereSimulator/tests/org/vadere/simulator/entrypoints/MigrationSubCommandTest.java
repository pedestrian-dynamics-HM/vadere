package org.vadere.simulator.entrypoints;

import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.projects.migration.MigrationAssistant;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MigrationSubCommandTest {

	private Path baseScenario;
	private Path baseScenarioBackup;
	private StringContains v01 = new StringContains("\"release\" : \"0.1\"");
	private StringContains vlatest = new StringContains("\"release\" : \"" + Version.latest().label() + "\"");
	private Path rootIgnore;
	private Path[] ignore = new Path[4];
	private Path[] ignoreBackup = new Path[4];

	@Before
	public void init() throws URISyntaxException, IOException {
		baseScenario = Paths.get(getClass()
				.getResource("/migration/VadererConsole/v0.1_to_LATEST_Test1.scenario").toURI());
		baseScenarioBackup = IOUtils.makeBackup(baseScenario, ".bak", true);

		rootIgnore = Paths.get(getClass()
				.getResource("/migration/VadererConsole/testDoNotMigrate").toURI());
		ignore[0] = rootIgnore.resolve("1").resolve("1.scenario");
		ignore[1] = rootIgnore.resolve("1/2").resolve("2.scenario");
		ignore[2] = rootIgnore.resolve("1/2/3").resolve("3.scenario");
		ignore[3] = rootIgnore.resolve("1/2/3/4").resolve("4.scenario");
		for (int i = 0; i < ignore.length; i++) {
			ignoreBackup[i] = IOUtils.makeBackup(ignore[i], ".bak", true);
		}
	}

	@After
	public void clenaup() throws IOException {
		if (baseScenario != null && baseScenarioBackup != null) {
			Files.copy(baseScenarioBackup, baseScenario, StandardCopyOption.REPLACE_EXISTING);
			Files.deleteIfExists(baseScenarioBackup);
		}

		for (int i = 0; i < ignore.length; i++) {
			if (ignore[i] != null && ignoreBackup[i] != null) {
				Path orig = ignore[i];
				Path bak = ignoreBackup[i];
				Files.copy(bak, orig, StandardCopyOption.REPLACE_EXISTING);
				Files.deleteIfExists(bak);
			}
		}

		Path legacyFile = MigrationAssistant.getBackupPath(baseScenario);
		Files.deleteIfExists(legacyFile);
	}

	/**
	 * Test if the supplied file will be migrated to the latest version and if the legacy file is
	 * create correctly.
	 */
	@Test
	public void testMigrateAndRevertSingleFile() throws IOException {

		assertThat("Old Version must be 0.1", getText(baseScenario), v01);
		String[] args = new String[]{SubCommand.MIGRATE.getCmdName(), baseScenario.toString()};
		VadereConsole.main(args);
		Path legacyFile = MigrationAssistant.getBackupPath(baseScenario);
		assertTrue("There must be legacyFile", legacyFile.toFile().exists());
		assertThat("New Version must be latest: " + Version.latest().toString(), getText(baseScenario), vlatest);

		args = new String[]{SubCommand.MIGRATE.getCmdName(), "--revert-migration", baseScenario.toString()};
		VadereConsole.main(args);
		assertThat("After revert the version must be 0.1", getText(baseScenario), v01);
		assertFalse("legacy file should be deleted after revert", legacyFile.toFile().exists());
	}

	@Test
	public void testMigrateAndRevertListOfFiles() throws IOException {

		Path f2 = Files.copy(baseScenario, baseScenario.getParent().resolve("copy.scenario"), StandardCopyOption.REPLACE_EXISTING);
		assertThat("Old Version must be 0.1", getText(baseScenario), v01);
		assertThat("Old Version must be 0.1", getText(f2), v01);
		String[] args = new String[]{SubCommand.MIGRATE.getCmdName(), baseScenario.toString(), f2.toString()};
		VadereConsole.main(args);
		Path legacyFile1 = MigrationAssistant.getBackupPath(baseScenario);
		Path legacyFile2 = MigrationAssistant.getBackupPath(f2);
		assertTrue("There must be legacyFile1", legacyFile1.toFile().exists());
		assertThat("New Version must be latest: " + Version.latest().toString(), getText(baseScenario), vlatest);
		assertTrue("There must be legacyFile2", legacyFile2.toFile().exists());
		assertThat("New Version must be latest: " + Version.latest().toString(), getText(f2), vlatest);

		args = new String[]{SubCommand.MIGRATE.getCmdName(), "--revert-migration", baseScenario.toString(), f2.toString()};
		VadereConsole.main(args);
		assertThat("File 1: After revert the version must be 0.1", getText(baseScenario), v01);
		assertFalse("File 1: legacy file should be deleted after revert", legacyFile1.toFile().exists());
		assertThat("File 2: After revert the version must be 0.1", getText(f2), v01);
		assertFalse("File 2: legacy file should be deleted after revert", legacyFile2.toFile().exists());

		Files.deleteIfExists(f2);

	}


	@Test
	public void testMigrationSameVersion() throws IOException {

		assertThat("Old Version must be 0.1", getText(baseScenario), v01);
		String[] args = new String[]{SubCommand.MIGRATE.getCmdName(), "--target-version", Version.V0_1.label(), baseScenario.toString()};
		VadereConsole.main(args);
		Path legacyFile = MigrationAssistant.getBackupPath(baseScenario);
		assertThat("New Version must be the same", getText(baseScenario), v01);

		assertFalse("No Transformation performed thus the should not be a legacyFile", legacyFile.toFile().exists());
	}

	/**
	 * Migrate a directory recursively
	 */
	@Test
	public void testIgnoreDirectoryAndDirectoryTreesRecursive() throws IOException {
		for (Path path : ignore) {
			assertThat("Old Version must be 0.1", getText(path), v01);
		}
		String[] args = new String[]{SubCommand.MIGRATE.getCmdName(), "-r", rootIgnore.toString()};
		VadereConsole.main(args);
		//only the second
		StringContains[] matcher = new StringContains[]{v01, vlatest, v01, v01};
		Boolean[] hasLegacyFile = new Boolean[]{false, true, false, false};
		for (int i = 0; i < ignore.length; i++) {
			assertThat("(" + String.valueOf(i + 1) + ") Version of file not as accepted", getText(ignore[i]), matcher[i]);
			Path legacy = MigrationAssistant.getBackupPath(ignore[i]);
			assertEquals("(" + String.valueOf(i + 1) + ") Existence of Backup File not as accented", legacy.toFile().exists(), hasLegacyFile[i]);
			Files.deleteIfExists(legacy);
		}

	}

	@Test
	public void testIgnoreDirectoryAndDirectoryTreesNoneRecursive() throws IOException {
		for (Path path : ignore) {
			assertThat("Old Version must be 0.1", getText(path), v01);
		}
		String[] args = new String[]{SubCommand.MIGRATE.getCmdName(), rootIgnore.toString()};
		VadereConsole.main(args);
		for (Path path : ignore) {
			assertThat("Old Version must be 0.1", getText(path), v01);
			Path legacy = MigrationAssistant.getBackupPath(path);
			assertFalse(Files.exists(legacy));
		}

	}

	private String getText(Path path) throws IOException {
		return IOUtils.readTextFile(path);
	}
}

