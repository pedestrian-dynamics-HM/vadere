package org.vadere.simulator.projects.migration.jsontranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Test;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JoltTransformV0toV1Test extends JsonTransformationTest {


	@Override
	public Path getTestDir() {
		return null;
	}

	// Source postHook should not bee used here
	@Test
	public void TestPostHooks1() throws MigrationException {
		JsonTransformation transformation = AbstractJsonTransformation.get(Version.NOT_A_RELEASE);
		String TEST1 = "/migration/vNOT-A-RELEASE_to_v0.1_Test1.scenario";
		JsonNode in = getJsonFromResource(TEST1);
		JsonNode out = transformation.applyAll(in);
		// will test that  sources exists.
		JsonNode sources = pathMustExist(out, "vadere/topography/sources");
		assertEquals("Therer must be one source", 1, sources.size());
		assertTrue("The source should not have the attribute distributionParameters", sources.elements().next().path("distributionParameters").isMissingNode());


		assertThat(pathMustExist(out, "vadere/mainModel"),
				nodeHasText("org.vadere.simulator.models.osm.OptimalStepsModel"));
		assertThat(pathMustExist(out, "vadere/attributesModel/org.vadere.state.attributes.models.AttributesOSM/pedestrianPotentialModel"),
				nodeHasText("org.vadere.simulator.models.potential.PotentialFieldPedestrianCompact"));
		assertThat(pathMustExist(out, "vadere/attributesModel/org.vadere.state.attributes.models.AttributesOSM/obstaclePotentialModel"),
				nodeHasText("org.vadere.simulator.models.potential.PotentialFieldObstacleCompact"));
	}


	// All postHooks should bee used here
	@Test
	public void TestPostHooks2() throws MigrationException {
		JsonTransformation transformation = factory.getJoltTransformV0toV1();
		String TEST2 = "/migration/vNOT-A-RELEASE_to_v0.1_Test2.scenario";
		JsonNode in = getJsonFromResource(TEST2);
		JsonNode out = transformation.applyAll(in);

		JsonNode sources = pathMustExist(out, "vadere/topography/sources");
		assertEquals("Therer must be one source", 1, sources.size());
		assertFalse("The source must have the attribute distributionParameters", sources.elements().next().path("distributionParameters").isMissingNode());

		assertThat(pathMustExist(out, "vadere/mainModel"),
				nodeHasText("org.vadere.simulator.models.osm.OptimalStepsModel"));
		assertThat(pathMustExist(out, "vadere/attributesModel/org.vadere.state.attributes.models.AttributesOSM/pedestrianPotentialModel"),
				nodeHasText("org.vadere.simulator.models.potential.PotentialFieldPedestrianCompact"));
		assertThat(pathMustExist(out, "vadere/attributesModel/org.vadere.state.attributes.models.AttributesOSM/obstaclePotentialModel"),
				nodeHasText("org.vadere.simulator.models.potential.PotentialFieldObstacleCompact"));

	}

	// should fail because no main model was found
	@Test(expected = MigrationException.class)
	public void TestPostHooks3() throws MigrationException {
		JsonTransformation transformation = factory.getJoltTransformV0toV1();
		String TEST3 = "/migration/vNOT-A-RELEASE_to_v0.1_Test3.scenario";
		JsonNode in = getJsonFromResource(TEST3);
		transformation.applyAll(in);

		fail("should not be reached! The Transformation should fail with MigrationException");
	}


}