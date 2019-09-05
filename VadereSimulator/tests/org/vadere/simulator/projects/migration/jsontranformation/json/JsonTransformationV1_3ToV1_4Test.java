package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.JsonTransformationTest;

import java.nio.file.Path;

public class JsonTransformationV1_3ToV1_4Test extends JsonTransformationTest {

	@Override
	public Path getTestDir() {
		return getPathFromResources("/migration/v1_3_to_v1_4");
	}

	@Test
	public void assertThatJsonTransformationV1_3ToV1_4RenamesFootStepsToStoreInTopography() throws MigrationException {
		String scenarioFileAsString = getTestFileAsString("s001.scenario");
		JsonNode oldScenarioAsJson = getJsonFromString(scenarioFileAsString);

		String oldJsonPath = "scenario/topography/attributesPedestrian/footStepsToStore";
		String newJsonPath = "scenario/topography/attributesPedestrian/footstepHistorySize";

		pathMustExist(oldScenarioAsJson, oldJsonPath);
		pathMustNotExist(oldScenarioAsJson, newJsonPath);

		JsonTransformationV1_3ToV1_4 transform = factory.getJsonTransformationV1_3ToV1_4();
		JsonNode newScenarioAsJson = transform.applyAll(oldScenarioAsJson);

		pathMustNotExist(newScenarioAsJson, oldJsonPath);
		pathMustExist(newScenarioAsJson, newJsonPath);
	}

	@Test
	public void assertThatJsonTransformationV1_3ToV1_4RenamesFootStepsToStoreInMainModel() throws MigrationException {
		String scenarioFileAsString = getTestFileAsString("s002.scenario");
		JsonNode oldScenarioAsJson = getJsonFromString(scenarioFileAsString);

		String oldJsonPath = "scenario/attributesModel/org.vadere.state.attributes.scenario.AttributesCar/footStepsToStore";
		String newJsonPath = "scenario/attributesModel/org.vadere.state.attributes.scenario.AttributesCar/footstepHistorySize";

		pathMustExist(oldScenarioAsJson, oldJsonPath);
		pathMustNotExist(oldScenarioAsJson, newJsonPath);

		JsonTransformationV1_3ToV1_4 transform = factory.getJsonTransformationV1_3ToV1_4();
		JsonNode newScenarioAsJson = transform.applyAll(oldScenarioAsJson);

		pathMustNotExist(newScenarioAsJson, oldJsonPath);
		pathMustExist(newScenarioAsJson, newJsonPath);
	}

}