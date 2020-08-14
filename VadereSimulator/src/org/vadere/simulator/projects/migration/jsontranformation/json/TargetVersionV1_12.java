package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.util.version.Version;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Remove node "threatMemory" under "scenario.topography.dynamicElements.psychologyStatus"
 */
@MigrationTransformation(targetVersionLabel = "1.12")
public class TargetVersionV1_12 extends SimpleJsonTransformation {

	public TargetVersionV1_12(){
		super(Version.V1_12);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookFirst(this::targetChangerUseProbabilityToChangeTargetList);
		addPostHookLast(this::sort);
	}

	public JsonNode targetChangerUseProbabilityToChangeTargetList(JsonNode node) throws MigrationException {

		// targetChangers may be missing if an old scenario was never opened since the targetChangers where introduced
		// This is ok, since a targetChangers node will be inserted if the scenario is read.
		if ( !path(node, "scenario/topography/targetChangers").isMissingNode()){
			Iterator<JsonNode> iter = iteratorTargetChangers(node);
			while (iter.hasNext()){
				JsonNode changer = iter.next();
				JsonNode probabilityToChangeTarget = pathMustExist(changer, "probabilityToChangeTarget");
				if (probabilityToChangeTarget.isDouble()){
					double val = probabilityToChangeTarget.asDouble();
					List<Double> valNew = new ArrayList<>();
					valNew.add(val);
					addArrayField(changer, "probabilityToChangeTarget", valNew);
				}
			}
		}

		return node;
	}

}
