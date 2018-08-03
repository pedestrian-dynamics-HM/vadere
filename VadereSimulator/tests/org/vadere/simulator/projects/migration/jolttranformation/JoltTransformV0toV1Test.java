package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Test;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.state.util.StateJsonConverter;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JoltTransformV0toV1Test extends JoltTransformationTest{

	private final String TRANSFORM = "/transform_vNOT-A-RELEASE_to_v0.1.json";
	private final String IDENTITY = "/identity_v0.1.json";

	// Source postHook should not bee used here
	@Test
	public void TestPostHooks1() throws IOException, MigrationException, URISyntaxException {
		JoltTransformation transformation = JoltTransformation.get(Version.NOT_A_RELEASE);
		String TEST1 = "/migration/vNOT-A-RELEASE_to_v0.1_Test1.scenario";
		JsonNode in = getJson(TEST1);
		JsonNode out = transformation.applyTransformation(in);
		JsonNode sources = out.path("vadere").path("topography").path("sources");
		assertFalse("There must be a source node", sources.isMissingNode());
		assertEquals("Therer must be one source",1, sources.size());
		assertTrue("The source should not have the attribute distributionParameters", sources.elements().next().path("distributionParameters").isMissingNode());


		assertEquals(out.path("vadere").path("mainModel").asText(),"org.vadere.simulator.models.osm.OptimalStepsModel");
		assertEquals(out.path("vadere").path("attributesModel")
						.path("org.vadere.state.attributes.models.AttributesOSM").path("pedestrianPotentialModel").asText(),
				"org.vadere.simulator.models.potential.PotentialFieldPedestrianCompact");
		assertEquals(out.path("vadere").path("attributesModel")
						.path("org.vadere.state.attributes.models.AttributesOSM").path("obstaclePotentialModel").asText(),
				"org.vadere.simulator.models.potential.PotentialFieldObstacleCompact");
	}


	// All postHooks should bee used here
	@Test
	public void TestPostHooks2() throws IOException, MigrationException, URISyntaxException {
		JoltTransformation transformation = new JoltTransformV0toV1(TRANSFORM, IDENTITY, Version.V0_1);
		String TEST2 = "/migration/vNOT-A-RELEASE_to_v0.1_Test2.scenario";
		JsonNode in = getJson(TEST2);
		JsonNode out = transformation.applyTransformation(in);

		JsonNode sources = out.path("vadere").path("topography").path("sources");
		assertFalse("There must be a source node", sources.isMissingNode());
		assertEquals("Therer must be one source",1, sources.size());
		assertFalse("The source must have the attribute distributionParameters", sources.elements().next().path("distributionParameters").isMissingNode());
		System.out.println(StateJsonConverter.writeValueAsString(out));

		assertEquals(out.path("vadere").path("mainModel").asText(),"org.vadere.simulator.models.osm.OptimalStepsModel");
		assertEquals(out.path("vadere").path("attributesModel")
				.path("org.vadere.state.attributes.models.AttributesOSM").path("pedestrianPotentialModel").asText(),
				"org.vadere.simulator.models.potential.PotentialFieldPedestrianCompact");
		assertEquals(out.path("vadere").path("attributesModel")
						.path("org.vadere.state.attributes.models.AttributesOSM").path("obstaclePotentialModel").asText(),
				"org.vadere.simulator.models.potential.PotentialFieldObstacleCompact");

	}

	// should fail because no main model was found
	@Test(expected = MigrationException.class)
	public void TestPostHooks3() throws IOException, MigrationException, URISyntaxException {
		JoltTransformation transformation = new JoltTransformV0toV1(TRANSFORM, IDENTITY, Version.V0_1);
		String TEST3 = "/migration/vNOT-A-RELEASE_to_v0.1_Test3.scenario";
		JsonNode in = getJson(TEST3);
		transformation.applyTransformation(in);

		fail("should not be reached! The Transformation should fail with MigrationException");
	}

}