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
@MigrationTransformation(targetVersionLabel = "1.14")
public class TargetVersionV1_14 extends SimpleJsonTransformation {

	public TargetVersionV1_14(){
		super(Version.V1_14);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookFirst(this::addStrategyLayer);
		addPostHookLast(this::sort);
	}

	public JsonNode addStrategyLayer(JsonNode node) throws MigrationException {

		String useStrategyModelKey = "attributesStrategy";

		JsonNode scenarioNode = path(node, "scenario");
		JsonNode strategyModel = path(scenarioNode, useStrategyModelKey);

		if (strategyModel.isMissingNode()) {

			ObjectNode n = (ObjectNode) scenarioNode;
			JsonNode attributesStrategyNode = StateJsonConverter.serializeAttributesStrategyModelToNode(new AttributesStrategyModel());
			n.set(useStrategyModelKey, attributesStrategyNode );

		}

		return node;

	}

}
