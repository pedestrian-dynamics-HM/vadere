package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;

/**
 * Remove node "perceivedThreat" under "scenario.topography.dynamicElements.psychologyStatus"
 */
@MigrationTransformation(targetVersionLabel = "1.9")
public class TargetVersionV1_9 extends SimpleJsonTransformation {

	public TargetVersionV1_9(){
		super(Version.V1_9);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookFirst(this::removePerceivedThreatNode);
		addPostHookLast(this::sort);
	}

	public JsonNode removePerceivedThreatNode(JsonNode node) throws MigrationException {
		String nodeNameToRemove = "perceivedThreat";

		JsonNode dynamicElementsNode = path(node, "scenario/topography/dynamicElements");

		if (dynamicElementsNode.isArray()) {
			for (JsonNode dynamicElementNode : dynamicElementsNode) {
				JsonNode psychologyStatusNode = path(dynamicElementNode, "psychologyStatus");

				removeIfExists(psychologyStatusNode, nodeNameToRemove);
			}
		}

		return node;
	}

}
