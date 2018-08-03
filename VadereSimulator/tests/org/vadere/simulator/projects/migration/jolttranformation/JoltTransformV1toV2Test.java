package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Test;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.state.util.StateJsonConverter;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class JoltTransformV1toV2Test extends JoltTransformationTest{

	private final String TRANSFORM = "/transform_v0.1_to_v0.2.json";
	private final String IDENTITY = "/identity_v0.2.json";


	@Test
	public void Test1() throws MigrationException, IOException, URISyntaxException {
		JoltTransformation t = JoltTransformation.get(Version.V0_1);
		JsonNode node = t.applyTransformation(getJson("/migration/v0.1_to_v0.2_Test1.scenario"));
		JsonNode apc = node.path("scenario").path("attributesModel").path("org.vadere.state.attributes.models.AttributesPotentialCompact");
		assertFalse(apc.isMissingNode());
		JsonNode osm = node.path("scenario").path("attributesModel").path("org.vadere.state.attributes.models.AttributesOSM");
		assertFalse(osm.isMissingNode());
		JsonNode aff = node.path("scenario").path("attributesModel").path("org.vadere.state.attributes.models.AttributesFloorField");
		assertFalse(aff.isMissingNode());

	}

	@Test
	public void Test2() throws MigrationException, IOException, URISyntaxException {
		JoltTransformation t = JoltTransformation.get(Version.V0_1);
		JsonNode node = t.applyTransformation(getJson("/migration/v0.1_to_v0.2_Test2.scenario"));
		JsonNode apc = node.path("scenario").path("attributesModel").path("org.vadere.state.attributes.models.AttributesPotentialCompact");
		assertFalse(apc.isMissingNode());
		JsonNode osm = node.path("scenario").path("attributesModel").path("org.vadere.state.attributes.models.AttributesOSM");
		assertFalse(osm.isMissingNode());
		JsonNode aff = node.path("scenario").path("attributesModel").path("org.vadere.state.attributes.models.AttributesFloorField");
		assertFalse(aff.isMissingNode());

	}

	// Test default values if only name is given.
	@Test
	public void Test3() throws MigrationException, IOException, URISyntaxException {
		JoltTransformation t = JoltTransformation.get(Version.V0_1);
		JsonNode node = t.applyTransformation(getJson("/migration/v0.1_to_v0.2_Test3.scenario"));
		assertTrue("should be there and empty",!node.path("scenario").path("attributesModel").isMissingNode());
		assertEquals("should be empty", 0, node.path("scenario").path("attributesModel").size());
		assertEquals("Name schould be set", "XXXX", node.path("name").asText());
	}


}