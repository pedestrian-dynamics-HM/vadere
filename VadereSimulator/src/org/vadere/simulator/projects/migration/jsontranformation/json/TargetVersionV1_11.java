package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Remove node "threatMemory" under "scenario.topography.dynamicElements.psychologyStatus"
 */
@MigrationTransformation(targetVersionLabel = "1.11")
public class TargetVersionV1_11 extends SimpleJsonTransformation {

	public TargetVersionV1_11(){
		super(Version.V1_11);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookFirst(this::targetChangerUseNextTargetList);
		addPostHookLast(this::sort);
	}

	public JsonNode targetChangerUseNextTargetList(JsonNode node) throws MigrationException {

		// targetChangers may be missing if an old scenario was never opened since the targetChangers where introduced
		// This is ok, since a targetChangers node will be inserted if the scenario is read.
		if ( !path(node, "scenario/topography/targetChangers").isMissingNode()){
			Iterator<JsonNode> iter = iteratorTargetChangers(node);
			while (iter.hasNext()){
				JsonNode changer = iter.next();
				JsonNode nextTarget = pathMustExist(changer, "nextTarget");
				if (nodeIsNumber(nextTarget)){
					int val = nextTarget.asInt();
					List<Integer> valNew = new ArrayList<>();
					valNew.add(val);
					addArrayField(changer, "nextTarget", valNew);
				}
			}
		}

		return node;
	}

}
