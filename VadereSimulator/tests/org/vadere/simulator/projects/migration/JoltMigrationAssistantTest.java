package org.vadere.simulator.projects.migration;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.After;
import org.junit.Test;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.io.IOUtils;
import org.vadere.util.io.RecursiveCopy;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import joptsimple.internal.Strings;

import static org.junit.Assert.assertEquals;

public class JoltMigrationAssistantTest {

	// clean up after test
	@After
	public void resetTestStructure() throws URISyntaxException {
		String dest = getClass().getResource("/migration/testProject_v0.1").toURI().getPath();
		String source = getClass().getResource("/migration/testProject_v0.1.bak").toURI().getPath();
		try {

			if (Paths.get(dest).toFile().exists()) {
				Files.walk(Paths.get(dest))
						.sorted(Comparator.reverseOrder())
						.map(Path::toFile)
						.forEach(File::delete);
			}
			Files.walkFileTree(Paths.get(source), new RecursiveCopy(source, dest));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Test transformation of single scenario file
	@Test
	public void TestTransform() throws IOException {

		String json = IOUtils.readTextFileFromResources("/migration/testProject_v0.1/scenarios/basic_1_chicken_osm1.scenario");
//		String json = IOUtils.readTextFileFromResources("/migration/testProject_v0.1/scenarios/WithDots_basic_1_chicken_osm1.scenario");
		JsonNode node = StateJsonConverter.deserializeToNode(json);

		JoltMigrationAssistant joltMigrationAssistant = new JoltMigrationAssistant();
		try {
			JsonNode newNode = joltMigrationAssistant.transform(node, Version.V0_2);
		} catch (MigrationException e) {
			e.printStackTrace();
		}


	}

	// Test project transformation
	@Test
	public void TestTransformProject() throws URISyntaxException, IOException {
		String projectPath = getClass().getResource("/migration/testProject_v0.1").toURI().getPath();

		JoltMigrationAssistant joltMigrationAssistant = new JoltMigrationAssistant();

		MigrationResult res = joltMigrationAssistant.analyzeProject(projectPath);
		assertEquals("", new MigrationResult(12, 1, 10, 1), res);
		System.out.println(Strings.repeat('#', 80));
	}

}