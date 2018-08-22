package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Test;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class JoltTransformV1toV2Test extends JoltTransformationTest {

	private final String TRANSFORM = "/transform_v0.1_to_v0.2.json";
	private final String IDENTITY = "/identity_v0.2.json";

	@Override
	protected Path getTestDir() {
		return null;
	}

	@Test
	public void Test1() throws MigrationException, IOException, URISyntaxException {
		JoltTransformation t = JoltTransformation.get(Version.V0_1);
		JsonNode node = t.applyTransformation(getJsonFromResource("/migration/v0.1_to_v0.2_Test1.scenario"));
		pathMustExist(node, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesPotentialCompact");
		pathMustExist(node, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesOSM");
		pathMustExist(node, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesFloorField");

	}

	@Test
	public void Test2() throws MigrationException, IOException, URISyntaxException {
		JoltTransformation t = JoltTransformation.get(Version.V0_1);
		JsonNode node = t.applyTransformation(getJsonFromResource("/migration/v0.1_to_v0.2_Test2.scenario"));
		pathMustExist(node, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesPotentialCompact");
		pathMustExist(node, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesOSM");
		pathMustExist(node, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesFloorField");

	}

	// Test default values if only name is given.
	@Test
	public void Test3() throws MigrationException, IOException, URISyntaxException {
		JoltTransformation t = JoltTransformation.get(Version.V0_1);
		JsonNode node = t.applyTransformation(getJsonFromResource("/migration/v0.1_to_v0.2_Test3.scenario"));
		pathMustExist(node, "scenario/attributesModel");
		assertEquals("should be empty", 0, pathMustExist(node, "scenario/attributesModel").size());
		assertThat(pathMustExist(node, "name"), nodeHasText("XXXX"));
	}

}