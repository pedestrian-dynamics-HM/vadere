package org.vadere.simulator.projects.migration.jsontranformation;

import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationAssistant;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.MigrationOptions;

import java.nio.file.Path;
import java.util.ArrayList;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class JoltTransformV3toV4Test extends JsonTransformationTest {

	private ArrayList<Path> scenarios;
	private ArrayList<Path> bakScenarios;
	private StringContains v04 = versionMatcher(Version.V0_4);
	private StringContains useFixedSeed = new StringContains("useFixedSeed");
	private StringContains fixedSeed = new StringContains("fixedSeed");
	private StringContains simulationSeed = new StringContains("simulationSeed");
	private StringContains useRandomSeed = new StringContains("useRandomSeed");
	private StringContains randomSeed = new StringContains("randomSeed");

	@Override
	public Path getTestDir() {
		return getPathFromResources("/migration/v03_to_v04");
	}

	@Before
	public void init() {
		super.init();

		scenarios = new ArrayList<>();
		bakScenarios = new ArrayList<>();

		scenarios.add(getRelativeTestPath("basic_1_chicken_osm1.scenario"));
		scenarios.add(getRelativeTestPath("group_OSM_1Source1Place.scenario"));

		bakScenarios.add(getTestFileBackup("basic_1_chicken_osm1.scenario"));
		bakScenarios.add(getTestFileBackup("group_OSM_1Source1Place.scenario"));
	}

	@After
	public void cleaUp() {
		super.cleaUp();
	}

	@Test
	public void TestTransform() {
		MigrationAssistant ma = MigrationAssistant.getNewInstance(MigrationOptions.defaultOptions());
		try {
			String out = ma.migrateScenarioFile(scenarios.get(0), Version.V0_4);
//			System.out.println(out);
			assertThat("New Version must be V0.4", out, v04);
			assertThat("New Entry missing", out, useFixedSeed);
			assertThat("New Entry missing", out, fixedSeed);
			assertThat("New Entry missing", out, simulationSeed);
			assertThat("Old Entry still there", out, not(useRandomSeed));
			assertThat("Old Entry still there", out, not(randomSeed));

		} catch (MigrationException e) {
			e.printStackTrace();
			fail();
		}

	}

}