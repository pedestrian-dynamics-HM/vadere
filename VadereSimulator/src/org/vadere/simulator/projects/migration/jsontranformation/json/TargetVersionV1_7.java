package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.state.attributes.AttributesPsychology;

/**
 * Introduce new node "scenario.attributesPsychology" into JSON.
 *
 * Move "scenario.attributesSimulation.usePsychologyLayer"
 * to this new node.
 *
 * This new node "scenario.attributesPsychology" holds
 * also the newly introduced "AttributesPsychologyLayer"
 * which is populated with default values when the migration
 * assistant runs.
 *
 * The final JSON looks like this then:
 *
 * {
 *    "name" : "ScenarioName",
 *    ...
 *    "scenario" : {
 *      "attributesSimulation": { ... },
 *      "attributesPsychology": {
 *        "usePsychologyLayer": true,
 *        "psychologyLayer": {
 *          "perception": "SimplePerceptionModel",
 *          "cognition": "CooperativeCognitionModel"
 *        }
 *      },
 *      ...
 *    }
 * }
 */
@MigrationTransformation(targetVersionLabel = "1.7")
public class TargetVersionV1_7 extends SimpleJsonTransformation {

	public TargetVersionV1_7(){
		super(Version.V1_7);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookFirst(this::moveUsePsychologyLayer);
		addPostHookLast(this::sort);
	}

	public JsonNode moveUsePsychologyLayer(JsonNode node) throws MigrationException {
		String usePsychologyLayerKey = "usePsychologyLayer";

		JsonNode attributesSimulationNode = path(node, "scenario/attributesSimulation");
		JsonNode usePsychologyLayerNode = path(attributesSimulationNode, usePsychologyLayerKey);

		if (!usePsychologyLayerNode.isMissingNode()) {
			remove(attributesSimulationNode, usePsychologyLayerKey);
		}

		String newPsychologyNodeName = AttributesPsychology.JSON_KEY;
		JsonNode scenarioNode = path(node, "scenario");

		ObjectNode newPsychologyNode = ((ObjectNode)scenarioNode).putObject(newPsychologyNodeName);
		newPsychologyNode.set(usePsychologyLayerKey, usePsychologyLayerNode);

		return node;
	}

}
