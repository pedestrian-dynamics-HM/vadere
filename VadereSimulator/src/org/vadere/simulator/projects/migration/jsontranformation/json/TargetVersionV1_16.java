package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.state.util.JacksonObjectMapper;
import org.vadere.util.version.Version;

/**
 * Remove node "threatMemory" under "scenario.topography.dynamicElements.psychologyStatus"
 */
@MigrationTransformation(targetVersionLabel = "1.16")
public class TargetVersionV1_16 extends SimpleJsonTransformation {

	JacksonObjectMapper mapper = new JacksonObjectMapper();


	public TargetVersionV1_16(){
		super(Version.V1_16);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookFirst(this::movePerceptionProbability);
		addPreHookLast(this::sort);
	}

	public JsonNode movePerceptionProbability(JsonNode node) throws MigrationException {

		removeRedudanteAttribute(node);
		addNewFieldToScenarioNode(node);

		return node;

	}

	private JsonNode addNewFieldToScenarioNode(final JsonNode node) {

		String key = "reactionProbabilities";
		JsonNode scenarioNode = node.get("scenario");

		if (path(scenarioNode, key).isMissingNode()) {
			((ObjectNode) scenarioNode).put(key, mapper.createArrayNode());
		}

		return node;


	}

	private JsonNode removeRedudanteAttribute(final JsonNode node) {
		String perceptionProbabilityKey = "perceptionProbability";
		JsonNode stimulusInfosNode = path(node, "scenario/stimulusInfos");

		if (stimulusInfosNode.isArray()) {
			for (JsonNode stimulusInfoNode : stimulusInfosNode) {
				JsonNode stimuliNode = path(stimulusInfoNode, "stimuli");
				if (stimuliNode.isArray()) {
					for (JsonNode stimulusNode : stimuliNode) {
						if (hasChild(stimulusNode, perceptionProbabilityKey)) {
							((ObjectNode) stimulusNode).remove(perceptionProbabilityKey);
						}

					}
				}
			}
		}

		return node;
	}

}
