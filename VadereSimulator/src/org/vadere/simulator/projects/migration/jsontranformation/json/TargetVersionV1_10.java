package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;

/**
 * Remove node "threatMemory" under "scenario.topography.dynamicElements.psychologyStatus"
 */
@MigrationTransformation(targetVersionLabel = "1.10")
public class TargetVersionV1_10 extends SimpleJsonTransformation {

	public TargetVersionV1_10(){
		super(Version.V1_10);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookFirst(this::removeThreatMemoryNode);
		addPostHookLast(this::sort);
	}

	public JsonNode removeThreatMemoryNode(JsonNode node) throws MigrationException {
		String nodeNameToRemove = "threatMemory";

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
