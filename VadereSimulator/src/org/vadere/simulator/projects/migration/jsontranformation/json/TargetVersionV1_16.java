package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.state.psychology.perception.json.ReactionProbability;
import org.vadere.state.psychology.perception.json.StimulusInfoStore;
import org.vadere.state.util.JacksonObjectMapper;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.version.Version;

import java.util.Collections;
import java.util.Iterator;

import static org.vadere.state.util.StateJsonConverter.serializeStimuliToNode;

/**
 * Remove node "threatMemory" under "scenario.topography.dynamicElements.psychologyStatus"
 */
@MigrationTransformation(targetVersionLabel = "1.16")
public class TargetVersionV1_16 extends SimpleJsonTransformation {

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

	private void addNewFieldToScenarioNode(final JsonNode node) {
		JsonNode scenarioNode = node.get("scenario");
		StimulusInfoStore stimulusInfoStore = StateJsonConverter.deserializeStimuliFromArrayNode(scenarioNode);

		if (stimulusInfoStore.getReactionProbabilities().isEmpty() && !stimulusInfoStore.getStimulusInfos().isEmpty()){
			stimulusInfoStore.setReactionProbabilities(Collections.singletonList(new ReactionProbability()));
		}

		ObjectNode node1 = serializeStimuliToNode(stimulusInfoStore);
		Iterator<String> iter = node1.fieldNames();
		while (iter.hasNext()) {
			String fieldName = iter.next();
			((ObjectNode)scenarioNode).put(fieldName, node1.get(fieldName));
		}
	}

	private void removeRedudanteAttribute(final JsonNode node) {
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
	}

}
