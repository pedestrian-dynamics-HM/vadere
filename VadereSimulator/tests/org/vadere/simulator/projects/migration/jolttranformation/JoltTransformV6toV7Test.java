package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.state.attributes.models.AttributesOSM;

import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class JoltTransformV6toV7Test extends  JoltTransformationTest{

	@Override
	public Path getTestDir() {
		return getPathFromResources("/migration/v06_to_v07");
	}

//	        "minStepLength" : 0.4625,
//					"maxStepDuration" : 1.7976931348623157E308,

	@Test
	public void testDefaultOSMAttriubteValues(){
		String group_OSM_1Source2Places = getTestFileAsString("group_OSM_1Source2Places");
		assertThat(group_OSM_1Source2Places, versionMatcher(Version.V0_6));
		JsonNode group_OSM_1Source2PlacesJson = getJsonFromString(group_OSM_1Source2Places);

		JoltTransformation t = factory.getJoltTransformV6toV7();
		JsonNode v7 = null;
		try {
			v7 = t.applyTransformation(group_OSM_1Source2PlacesJson);
		} catch (MigrationException e){
			fail("Should not fail with MigrationException: " + e.getMessage());
		}
		assertThat(pathMustExist(v7, "release"), nodeHasText(Version.V0_7.label()));

		AttributesOSM attr = new AttributesOSM();
		double minStepLength = pathMustExist(v7, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesOSM/minStepLength").asDouble();
		double maxStepDuration =  pathMustExist(v7, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesOSM/maxStepDuration").asDouble();

		assertEquals(minStepLength, attr.getMinStepLength(), 0.001);
		assertEquals(maxStepDuration, attr.getMaxStepDuration(), Double.MAX_VALUE);

	}

	@Test
	public void useStepLengthInterceptAsDefault(){
		String group_OSM_1Source2Places = getTestFileAsString("group_OSM_1Source2Places_B");
		assertThat(group_OSM_1Source2Places, versionMatcher(Version.V0_6));
		JsonNode group_OSM_1Source2PlacesJson = getJsonFromString(group_OSM_1Source2Places);

		JoltTransformation t = factory.getJoltTransformV6toV7();
		JsonNode v7 = null;
		try {
			v7 = t.applyTransformation(group_OSM_1Source2PlacesJson);
		} catch (MigrationException e){
			fail("Should not fail with MigrationException: " + e.getMessage());
		}
		assertThat(pathMustExist(v7, "release"), nodeHasText(Version.V0_7.label()));

		AttributesOSM attr = new AttributesOSM();
		double minStepLength = pathMustExist(v7, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesOSM/minStepLength").asDouble();
		double maxStepDuration =  pathMustExist(v7, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesOSM/maxStepDuration").asDouble();

		assertEquals(minStepLength, 0.999, 0.001);
		assertEquals(maxStepDuration, attr.getMaxStepDuration(), Double.MAX_VALUE);
	}

	@Test
	public void PedestrianDensityGaussianProcessor(){
		String group_OSM_1Source2Places = getTestFileAsString("group_OSM_1Source1Place");
		assertThat(group_OSM_1Source2Places, versionMatcher(Version.V0_6));
		JsonNode group_OSM_1Source2PlacesJson = getJsonFromString(group_OSM_1Source2Places);

		JoltTransformation t = factory.getJoltTransformV6toV7();
		JsonNode v7 = null;
		try {
			v7 = t.applyTransformation(group_OSM_1Source2PlacesJson);
		} catch (MigrationException e){
			fail("Should not fail with MigrationException: " + e.getMessage());
		}
		assertThat(pathMustExist(v7, "release"), nodeHasText(Version.V0_7.label()));

		AttributesOSM attr = new AttributesOSM();
		double minStepLength = pathMustExist(v7, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesOSM/minStepLength").asDouble();
		double maxStepDuration =  pathMustExist(v7, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesOSM/maxStepDuration").asDouble();

		assertEquals(minStepLength, attr.getMinStepLength(), 0.001);
		assertEquals(maxStepDuration, attr.getMaxStepDuration(), Double.MAX_VALUE);

		ArrayList<JsonNode> pedestrianDensityGaussianProcessorAttrs =
				getProcessorsByType(v7, "org.vadere.simulator.projects.dataprocessing.processor.PedestrianDensityGaussianProcessor");

		assertThat(pedestrianDensityGaussianProcessorAttrs.size(), Is.is(1));
		JsonNode jsonAttr = pathMustExist(pedestrianDensityGaussianProcessorAttrs.get(0), "attributes");
		assertThat(jsonAttr.has("standardDeviation"), Is.is(true));
		assertThat(jsonAttr.has("standardDerivation"), Is.is(false));
	}
}