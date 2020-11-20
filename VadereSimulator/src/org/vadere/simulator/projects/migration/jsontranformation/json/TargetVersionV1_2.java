package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;

@MigrationTransformation(targetVersionLabel = "1.2")
public class TargetVersionV1_2 extends SimpleJsonTransformation {

	public TargetVersionV1_2(){
		super(Version.V1_2);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookFirst(this::changeCacheCSVtoTXT);
		addPostHookLast(this::sort);
	}

	public JsonNode changeCacheCSVtoTXT(JsonNode node) throws MigrationException {
		JsonNode attFloorField = path(node, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesFloorField");
		if (!attFloorField.isMissingNode()){
			JsonNode cacheType = path(attFloorField, "cacheType");
			if (!cacheType.isMissingNode()){
				if (cacheType.textValue().equals("CSV_CACHE")){
					changeStringValue(attFloorField, "cacheType", "TXT_CACHE");
				}
			}
		}
		return node;
	}
}
