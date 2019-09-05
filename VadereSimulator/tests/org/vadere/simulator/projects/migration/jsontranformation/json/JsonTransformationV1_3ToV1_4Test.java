package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
		String scenarioFileAsString = getTestFileAsString("footStepsToStoreInTopography.scenario");
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
		String scenarioFileAsString = getTestFileAsString("footStepsToStoreInMainModel.scenario");
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

	@Test
	public void assertThatJsonTransformationV1_3ToV1_4RenamesFootStepsToStoreInDynamicElements() throws MigrationException {
		String scenarioFileAsString = getTestFileAsString("footStepsToStoreInDynamicElements.scenario");
		JsonNode oldScenarioAsJson = getJsonFromString(scenarioFileAsString);

		JsonNode dynamicElementsNode = path(oldScenarioAsJson, "scenario/topography/dynamicElements");

		// Assert that "attributes" node in "dynamicElements" is NOT renamed here.
		if (dynamicElementsNode.isArray()) {
			for (JsonNode jsonNode : dynamicElementsNode) {
				JsonNode attributesNode = path(jsonNode, "attributes");

				if (!attributesNode.isMissingNode()) {
					pathMustExist(attributesNode, "footStepsToStore");
				}
			}
		}

		JsonTransformationV1_3ToV1_4 transform = factory.getJsonTransformationV1_3ToV1_4();
		JsonNode newScenarioAsJson = transform.applyAll(oldScenarioAsJson);

		// Assert that "attributes" node in "dynamicElements" is renamed here.
		if (dynamicElementsNode.isArray()) {
			for (JsonNode jsonNode : dynamicElementsNode) {
				JsonNode attributesNode = path(jsonNode, "attributes");

				if (!attributesNode.isMissingNode()) {
					pathMustExist(attributesNode, "footstepHistorySize");
				}
			}
		}
	}

}