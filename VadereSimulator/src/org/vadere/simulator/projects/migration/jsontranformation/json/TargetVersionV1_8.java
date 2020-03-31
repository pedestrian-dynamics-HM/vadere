package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;

/**
 * Rename stimulus "Bang" to general term "Threat" under node "stimulusInfos":
 *
 * <pre>
 * "stimulusInfos" : [ {
 *       "timeframe" : {...},
 *       "stimuli" : [ {
 *         "type" : "Bang",
 *         ...
 *       } ]
 *     } ]
 * </pre>
 */
@MigrationTransformation(targetVersionLabel = "1.8")
public class TargetVersionV1_8 extends SimpleJsonTransformation {

	public TargetVersionV1_8(){
		super(Version.V1_8);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookFirst(this::renameBangToThreat);
		addPostHookLast(this::sort);
	}

	public JsonNode renameBangToThreat(JsonNode node) throws MigrationException {
		String oldName = "Bang";
		String newName = "Threat";

		JsonNode stimulusInfosNode = path(node, "scenario/stimulusInfos");

		if (stimulusInfosNode.isArray()) {
			for (JsonNode stimulusInfoNode : stimulusInfosNode) {
				JsonNode stimuliNode = path(stimulusInfoNode, "stimuli");

				if (stimuliNode.isArray()) {
					for (JsonNode stimulusNode : stimuliNode) {
						if (stimulusNode.get("type").asText("").equals(oldName)) {
							changeStringValue(stimulusNode, "type", newName);
						}
					}
				}
			}
		}

		return node;
	}

}
