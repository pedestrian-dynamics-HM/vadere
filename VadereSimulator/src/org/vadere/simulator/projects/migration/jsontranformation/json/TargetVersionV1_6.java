package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;

/**
 * Rename following pedestrian attributes:
 * - "targetOrientationAngleThreshold" to "walkingDirectionSameIfAngleLessOrEqual"
 * - "angleCalculationType" to "walkingDirectionCalculation"
 *   Also adapt possible values for "walkingDirectionCalculation".
 * - "psychology" to "psychologyStatus"
 */
@MigrationTransformation(targetVersionLabel = "1.6")
public class TargetVersionV1_6 extends SimpleJsonTransformation {

	public TargetVersionV1_6(){
		super(Version.V1_6);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookFirst(this::renameTargetOrientationAngleThreshold);
		addPostHookFirst(this::renameAngleCalculationType);
		addPostHookFirst(this::renamePsychology);
		addPostHookLast(this::sort);
	}

	public JsonNode renameTargetOrientationAngleThreshold(JsonNode node) throws MigrationException {
		String oldName = "targetOrientationAngleThreshold";
		String newName = "walkingDirectionSameIfAngleLessOrEqual";

		renameInTopographyUnderAttributesPedestrian(node, oldName, newName);
		renameInTopographyUnderAttributesCar(node, oldName, newName);
		renameInMainModel(node, oldName, newName);
		renameInDynamicElements(node, oldName, newName);

		return node;
	}

	public JsonNode renameAngleCalculationType(JsonNode node) throws MigrationException {
		String oldName = "angleCalculationType";
		String newName = "walkingDirectionCalculation";

		renameInTopographyUnderAttributesPedestrian(node, oldName, newName);
		renameInTopographyUnderAttributesCar(node, oldName, newName);
		renameInMainModel(node, oldName, newName);
		renameInDynamicElements(node, oldName, newName);

		return node;
	}

	public JsonNode renamePsychology(JsonNode node) throws MigrationException {
		String oldName = "psychology";
		String newName = "psychologyStatus";

		JsonNode dynamicElementsNode = path(node, "scenario/topography/dynamicElements");

		if (dynamicElementsNode.isArray()) {
			for (JsonNode dynamicElementNode : dynamicElementsNode) {
				JsonNode psychologyNode = path(dynamicElementNode, "psychology");

				if (!psychologyNode.isMissingNode()) {
					renameField((ObjectNode)dynamicElementNode, oldName, newName);
				}
			}
		}

		return node;
	}

	private void renameInTopographyUnderAttributesPedestrian(JsonNode node, String oldName, String newName) throws MigrationException {
		JsonNode attributesPedestrianNode = pathMustExist(node, "scenario/topography/attributesPedestrian");
		renameField((ObjectNode)attributesPedestrianNode, oldName, newName);
		adaptValuesForWalkingDirectionCalculation(attributesPedestrianNode);
	}

	private void renameInTopographyUnderAttributesCar(JsonNode node, String oldName, String newName) throws MigrationException {
		JsonNode attributesPedestrianNode = pathMustExist(node, "scenario/topography/attributesCar");

		if (attributesPedestrianNode.asText() != "null") {
			renameField((ObjectNode) attributesPedestrianNode, oldName, newName);
			adaptValuesForWalkingDirectionCalculation(attributesPedestrianNode);
		}
	}

	private void renameInMainModel(JsonNode node, String oldName, String newName) throws MigrationException {
		JsonNode attributesModelNode = path(node, "scenario/attributesModel/org.vadere.state.attributes.scenario.AttributesCar");

		if (!attributesModelNode.isMissingNode()) {
			renameField((ObjectNode)attributesModelNode, oldName, newName);
			adaptValuesForWalkingDirectionCalculation(attributesModelNode);
		}
	}

	private void renameInDynamicElements(JsonNode node, String oldName, String newName) throws MigrationException {
		JsonNode dynamicElementsNode = path(node, "scenario/topography/dynamicElements");

		if (dynamicElementsNode.isArray()) {
			for (JsonNode jsonNode : dynamicElementsNode) {
				JsonNode attributesNode = path(jsonNode, "attributes");

				if (!attributesNode.isMissingNode()) {
					renameField((ObjectNode)attributesNode, oldName, newName);
					adaptValuesForWalkingDirectionCalculation(attributesNode);
				}
			}
		}
	}

	private void adaptValuesForWalkingDirectionCalculation(JsonNode parent) throws MigrationException {
		String[] oldValues = new String[] { "USE_CENTER", "USE_CLOSEST_POINT" };
		String[] newValues = new String[] { "BY_TARGET_CENTER", "BY_TARGET_CLOSEST_POINT" };

		for (int i = 0; i < oldValues.length; i++) {
			String oldValue = oldValues[i];
			String newValue = newValues[i];

			String key = "walkingDirectionCalculation";
			JsonNode walkingDirectionNode = parent.get(key);

			if (walkingDirectionNode != null) {
				if (walkingDirectionNode.asText("").equals(oldValue)) {
					changeStringValue(parent, key, newValue);
				}
			}
		}
	}

}
