package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;

@MigrationTransformation(targetVersionLabel = "1.4")
public class JsonTransformationV1_3ToV1_4 extends SimpleJsonTransformation {

	public JsonTransformationV1_3ToV1_4(){
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

		renameInTopography(node, oldName, newName);
		renameInMainModel(node, oldName, newName);

		return node;
	}

	private void renameInTopography(JsonNode node, String oldName, String newName) throws MigrationException {
		JsonNode attributesPedestrianNode = pathMustExist(node, "scenario/topography/attributesPedestrian");
		renameField((ObjectNode)attributesPedestrianNode, oldName, newName);
	}

	private void renameInMainModel(JsonNode node, String oldName, String newName) throws MigrationException {
		JsonNode attributesModelNode = path(node, "scenario/attributesModel/org.vadere.state.attributes.scenario.AttributesCar");

		if (!attributesModelNode.isMissingNode()) {
			renameField((ObjectNode)attributesModelNode, oldName, newName);
		}
	}
}
