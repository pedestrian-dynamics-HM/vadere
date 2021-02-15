package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.state.attributes.AttributesStrategyModel;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.version.Version;

/**
 * Remove node "threatMemory" under "scenario.topography.dynamicElements.psychologyStatus"
 */
@MigrationTransformation(targetVersionLabel = "1.15")
public class TargetVersionV1_15 extends SimpleJsonTransformation {

	public TargetVersionV1_15(){
		super(Version.V1_15);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookFirst(this::addStrategyLayer);
		addPostHookLast(this::sort);
	}

	public JsonNode addStrategyLayer(JsonNode node) throws MigrationException {

		String useStrategyModelKey = "attributesStrategy";

		JsonNode scenarioNode = path(node, "scenario");

		if (hasChild(scenarioNode, useStrategyModelKey)) {

			ObjectNode n = (ObjectNode) scenarioNode;
			n.remove(useStrategyModelKey);

		}

		return node;

	}

}
