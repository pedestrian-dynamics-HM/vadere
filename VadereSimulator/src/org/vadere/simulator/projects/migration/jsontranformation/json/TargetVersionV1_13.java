package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.util.version.Version;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@MigrationTransformation(targetVersionLabel =  "1.13")
public class TargetVersionV1_13 extends SimpleJsonTransformation {

	public TargetVersionV1_13() {
		super(Version.V1_13);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookFirst(this::targetChangerAlgorithm);
		addPreHookLast(this::sort);
	}

	private JsonNode targetChangerAlgorithm(JsonNode node) throws MigrationException {
		// targetChangers may be missing if an old scenario was never opened since the targetChangers where introduced
		// This is ok, since a targetChangers node will be inserted if the scenario is read.
		if ( !path(node, "scenario/topography/targetChangers").isMissingNode()){
			Iterator<JsonNode> iter = iteratorTargetChangers(node);
			while (iter.hasNext()){
				ObjectNode changer = (ObjectNode)iter.next();
				boolean pedAsTarget = remove(changer, "nextTargetIsPedestrian").asBoolean();
				if (pedAsTarget){
					// use FOLLOW_PERSON algorim
					changer.put("changeAlgorithmType", "FOLLOW_PERSON");
				} else {
					ArrayList<Integer> nextTarget = getIntegerList(changer, "nextTarget");
					ArrayList<Double>  probabilityToChangeTarget = getDoubleList(changer, "probabilityToChangeTarget");
					if (probabilityToChangeTarget.size() < nextTarget.size()){
						// use SELECT_LIST
						changer.put("changeAlgorithmType", "SELECT_LIST");
					} else if ((probabilityToChangeTarget.size() == nextTarget.size()) && (nextTarget.size() == 1)) {
						// use SELECT_LIST
						changer.put("changeAlgorithmType", "SELECT_LIST");
					} else if (probabilityToChangeTarget.size() == nextTarget.size()){
						// use SORTED_SUB_LIST
						changer.put("changeAlgorithmType", "SORTED_SUB_LIST");
					} else {
						// uaw SELECT_ELEMENT
						changer.put("changeAlgorithmType", "SELECT_ELEMENT");
					}
				}
			}
		}

		return node;
	}
}
