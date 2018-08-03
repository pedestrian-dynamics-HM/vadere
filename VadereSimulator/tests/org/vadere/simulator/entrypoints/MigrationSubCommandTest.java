package org.vadere.simulator.entrypoints;

import net.sourceforge.argparse4j.inf.ArgumentParserException;

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MigrationSubCommandTest {

	private Path baseScenario;
	private Path baseScenarioBackup;
	private StringContains v01 = new StringContains("\"release\" : \"0.1\"");
	//	StringContains v02 = new StringContains("\"release\" : \"0.2\"");
	private StringContains vlatest = new StringContains("\"release\" : \"" + Version.latest().label() + "\"");


	@Before
	public void init() throws URISyntaxException, IOException {
		baseScenario = Paths.get(getClass()
				.getResource("/migration/VadererConsole/v0.1_to_LATEST_Test1.scenario").toURI());
		baseScenarioBackup = IOUtils.makeBackup(baseScenario, ".bak", true);
	}

	@After
	public void clenaup() throws IOException {
		if (baseScenario != null && baseScenarioBackup != null){
			Files.copy(baseScenarioBackup, baseScenario, StandardCopyOption.REPLACE_EXISTING);
			Files.deleteIfExists(baseScenarioBackup);
		}

		Path legacyFile = MigrationAssistant.getBackupPath(baseScenario);
		Files.deleteIfExists(legacyFile);
	}

	/**
	 * Test if the supplied file will be migrated to the latest version and if the legacy file
	 * is create correctly.
	 */
	@Test
	public void testMigrationOptions() throws IOException {

		assertThat("Old Version must be 0.1", getText(baseScenario), v01);
		String[] args = new String[]{SubCommand.MIGRATE.getCmdName(), "-f", baseScenario.toString()};
		VadereConsole.main(args);
		Path legacyFile = MigrationAssistant.getBackupPath(baseScenario);
		assertTrue("There must be legacyFile", legacyFile.toFile().exists());
		assertThat("New Version must be latest: " + Version.latest().toString(), getText(baseScenario), vlatest);

		args = new String[]{SubCommand.MIGRATE.getCmdName(), "-f", baseScenario.toString(), "--revert-migration"};
		VadereConsole.main(args);
		assertThat("After revert the version must be 0.1", getText(baseScenario), v01);
		assertFalse("legacy file should be deleted after revert", legacyFile.toFile().exists());
	}


	private String getText (Path path) throws IOException {
		return IOUtils.readTextFile(path);
	}
}

