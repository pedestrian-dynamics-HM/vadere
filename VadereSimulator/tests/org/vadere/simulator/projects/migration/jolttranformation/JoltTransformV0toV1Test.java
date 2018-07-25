package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Test;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JoltTransformV0toV1Test {

	private final String TRANSFORM = "/transform_v0_to_v1.json";
	private final String IDENTITY = "/identity_v1.json";

	private JsonNode getJson(String resources) throws IOException, URISyntaxException {
		URL url = getClass().getResource(resources);
		String json = IOUtils.readTextFile(Paths.get(url.toURI()));
		return StateJsonConverter.deserializeToNode(json);
	}

	// Source postHook should not bee used here
	@Test
	public void TestPostHooks1() throws IOException, MigrationException, URISyntaxException {
		JoltTransformation transformation = new JoltTransformV0toV1(TRANSFORM, IDENTITY, Version.V0_1);
		String TEST1 = "/migration/Test1.scenario";
		JsonNode in = getJson(TEST1);
		JsonNode out = transformation.applyTransformation(in);
		JsonNode sources = out.path("scenario").path("topography").path("sources");
		assertFalse("There must be a source node", sources.isMissingNode());
		assertEquals("Therer must be one source",1, sources.size());
		assertTrue("The source should not have the attribute distributionParameters", sources.elements().next().path("distributionParameters").isMissingNode());


		assertEquals(out.path("scenario").path("mainModel").asText(),"org.vadere.simulator.models.osm.OptimalStepsModel");
		assertEquals(out.path("scenario").path("attributesModel")
						.path("org.vadere.state.attributes.models.AttributesOSM").path("pedestrianPotentialModel").asText(),
				"org.vadere.simulator.models.potential.PotentialFieldPedestrianCompact");
		assertEquals(out.path("scenario").path("attributesModel")
						.path("org.vadere.state.attributes.models.AttributesOSM").path("obstaclePotentialModel").asText(),
				"org.vadere.simulator.models.potential.PotentialFieldObstacleCompact");
	}


	// All postHooks should bee used here
	@Test
	public void TestPostHooks2() throws IOException, MigrationException, URISyntaxException {
		JoltTransformation transformation = new JoltTransformV0toV1(TRANSFORM, IDENTITY, Version.V0_1);
		String TEST2 = "/migration/Test2.scenario";
		JsonNode in = getJson(TEST2);
		JsonNode out = transformation.applyTransformation(in);

		JsonNode sources = out.path("scenario").path("topography").path("sources");
		assertFalse("There must be a source node", sources.isMissingNode());
		assertEquals("Therer must be one source",1, sources.size());
		assertFalse("The source must have the attribute distributionParameters", sources.elements().next().path("distributionParameters").isMissingNode());
		System.out.println(StateJsonConverter.writeValueAsString(out));

		assertEquals(out.path("scenario").path("mainModel").asText(),"org.vadere.simulator.models.osm.OptimalStepsModel");
		assertEquals(out.path("scenario").path("attributesModel")
				.path("org.vadere.state.attributes.models.AttributesOSM").path("pedestrianPotentialModel").asText(),
				"org.vadere.simulator.models.potential.PotentialFieldPedestrianCompact");
		assertEquals(out.path("scenario").path("attributesModel")
						.path("org.vadere.state.attributes.models.AttributesOSM").path("obstaclePotentialModel").asText(),
				"org.vadere.simulator.models.potential.PotentialFieldObstacleCompact");

	}

	// should fail because no main model was found
	@Test(expected = MigrationException.class)
	public void TestPostHooks3() throws IOException, MigrationException, URISyntaxException {
		JoltTransformation transformation = new JoltTransformV0toV1(TRANSFORM, IDENTITY, Version.V0_1);
		String TEST3 = "/migration/Test3.scenario";
		JsonNode in = getJson(TEST3);
		transformation.applyTransformation(in);

		fail("should not be reached! The Transformation should fail with MigrationException");
	}

}