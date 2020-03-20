package org.vadere.simulator.projects.migration.jsontranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Test;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class JoltTransformV1toV2Test extends JsonTransformationTest {

	@Override
	public Path getTestDir() {
		return null;
	}

	@Test
	public void Test1() throws MigrationException {
		JsonTransformation t = AbstractJsonTransformation.get(Version.V0_1);
		JsonNode node = t.applyAll(getJsonFromResource("/migration/v0.1_to_v0.2_Test1.scenario"));
		pathMustExist(node, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesPotentialCompact");
		pathMustExist(node, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesOSM");
		pathMustExist(node, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesFloorField");

	}

	@Test
	public void Test2() throws MigrationException {
		JsonTransformation t = AbstractJsonTransformation.get(Version.V0_1);
		JsonNode node = t.applyAll(getJsonFromResource("/migration/v0.1_to_v0.2_Test2.scenario"));
		pathMustExist(node, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesPotentialCompact");
		pathMustExist(node, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesOSM");
		pathMustExist(node, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesFloorField");

	}

	// Test default values if only name is given.
	@Test
	public void Test3() throws MigrationException {
		JsonTransformation t = AbstractJsonTransformation.get(Version.V0_1);
		JsonNode node = t.applyAll(getJsonFromResource("/migration/v0.1_to_v0.2_Test3.scenario"));
		pathMustExist(node, "scenario/attributesModel");
		assertEquals("should be empty", 0, pathMustExist(node, "scenario/attributesModel").size());
		assertThat(pathMustExist(node, "name"), nodeHasText("XXXX"));
	}

}