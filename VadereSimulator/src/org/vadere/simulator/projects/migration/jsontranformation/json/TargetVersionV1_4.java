package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;

@MigrationTransformation(targetVersionLabel = "1.4")
public class TargetVersionV1_4 extends SimpleJsonTransformation {

	public TargetVersionV1_4(){
		super(Version.V1_4);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookFirst(this::renameFootStepsToStore);
		addPostHookLast(this::sort);
	}

	public JsonNode renameFootStepsToStore(JsonNode node) throws MigrationException {
		String oldName = "footStepsToStore";
		String newName = "footstepHistorySize";

		renameInTopographyUnderAttributesPedestrian(node, oldName, newName);
		renameInTopographyUnderAttributesCar(node, oldName, newName);
		renameInMainModel(node, oldName, newName);
		renameInDynamicElements(node, oldName, newName);

		return node;
	}

	private void renameInTopographyUnderAttributesPedestrian(JsonNode node, String oldName, String newName) throws MigrationException {
		JsonNode attributesPedestrianNode = pathMustExist(node, "scenario/topography/attributesPedestrian");
		renameField((ObjectNode)attributesPedestrianNode, oldName, newName);
	}

	private void renameInTopographyUnderAttributesCar(JsonNode node, String oldName, String newName) throws MigrationException {
		JsonNode attributesPedestrianNode = pathMustExist(node, "scenario/topography/attributesCar");

		if (attributesPedestrianNode.asText() != "null") {
			renameField((ObjectNode) attributesPedestrianNode, oldName, newName);
		}
	}

	private void renameInMainModel(JsonNode node, String oldName, String newName) throws MigrationException {
		JsonNode attributesModelNode = path(node, "scenario/attributesModel/org.vadere.state.attributes.scenario.AttributesCar");

		if (!attributesModelNode.isMissingNode()) {
			renameField((ObjectNode)attributesModelNode, oldName, newName);
		}
	}

	private void renameInDynamicElements(JsonNode node, String oldName, String newName) throws MigrationException {
		JsonNode dynamicElementsNode = path(node, "scenario/topography/dynamicElements");

		if (dynamicElementsNode.isArray()) {
			for (JsonNode jsonNode : dynamicElementsNode) {
				JsonNode attributesNode = path(jsonNode, "attributes");

				if (!attributesNode.isMissingNode()) {
					renameField((ObjectNode)attributesNode, oldName, newName);
				}
			}
		}
	}
}
