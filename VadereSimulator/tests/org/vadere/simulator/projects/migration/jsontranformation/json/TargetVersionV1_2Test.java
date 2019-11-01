package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Test;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.JsonTransformationTest;

import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class TargetVersionV1_2Test extends JsonTransformationTest {

	@Override
	public Path getTestDir() {
		return getPathFromResources("/migration/v1_1_to_v1_2");
	}


	@Test
	public void csvToTxt() throws MigrationException {
		String jsonStr = getTestFileAsString("s002.scenario");
		JsonNode old = getJsonFromString(jsonStr);
		String typeOld =pathMustExist(old, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesFloorField/cacheType").asText();
		assertThat(typeOld, equalTo("CSV_CACHE"));


		TargetVersionV1_2 transform = factory.getTargetVersionV1_2();
		JsonNode jsonNew = transform.applyAll(old);

		String typeNew =pathMustExist(jsonNew, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesFloorField/cacheType").asText();
		assertThat(typeNew, equalTo("TXT_CACHE"));
	}

	@Test
	public void binToBin() throws MigrationException {
		String jsonStr = getTestFileAsString("s001.scenario");
		JsonNode old = getJsonFromString(jsonStr);
		String typeOld =pathMustExist(old, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesFloorField/cacheType").asText();
		assertThat(typeOld, equalTo("BIN_CACHE"));


		TargetVersionV1_2 transform = factory.getTargetVersionV1_2();
		JsonNode jsonNew = transform.applyAll(old);

		String typeNew =pathMustExist(jsonNew, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesFloorField/cacheType").asText();
		assertThat(typeNew, equalTo("BIN_CACHE"));
	}
}