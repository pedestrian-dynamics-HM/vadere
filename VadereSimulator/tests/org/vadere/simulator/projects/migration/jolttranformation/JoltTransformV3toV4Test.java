package org.vadere.simulator.projects.migration.jolttranformation;

import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.projects.migration.MigrationAssistant;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.MigrationOptions;
import org.vadere.util.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import org.vadere.simulator.entrypoints.Version;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;

public class JoltTransformV3toV4Test {

	ArrayList<Path> scenarios;
	ArrayList<Path> bakScenarios;
	private StringContains v04 = new StringContains("\"release\" : \"0.4\"");
	private StringContains useFixedSeed = new StringContains("useFixedSeed");
	private StringContains fixedSeed = new StringContains("fixedSeed");
	private StringContains simulationSeed = new StringContains("simulationSeed");
	private StringContains useRandomSeed = new StringContains("useRandomSeed");
	private StringContains randomSeed = new StringContains("randomSeed");



	@Before
	public void init() throws URISyntaxException {
		scenarios = new ArrayList<>();
		bakScenarios = new ArrayList<>();

		scenarios.add(Paths.get(getClass()
		.getResource("/migration/v03_to_v04/basic_1_chicken_osm1.scenario").toURI()));

		scenarios.add(Paths.get(getClass()
				.getResource("/migration/v03_to_v04/group_OSM_1Source1Place.scenario").toURI()));

		scenarios.forEach(f -> {
			try {
				bakScenarios.add(IOUtils.makeBackup(f, ".bak", true));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

	}

	@After
	public void cleaUp() throws IOException {
		for (int i=0; i < scenarios.size(); i++){
			if (scenarios.get(i) != null && bakScenarios.get(i) != null){
				Files.move(bakScenarios.get(i), scenarios.get(i), StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	@Test
	public void TestTransform(){
		MigrationAssistant ma = MigrationAssistant.getNewInstance(MigrationOptions.defaultOptions());
		try {
			String out = ma.convertFile(scenarios.get(0), Version.V0_4);
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