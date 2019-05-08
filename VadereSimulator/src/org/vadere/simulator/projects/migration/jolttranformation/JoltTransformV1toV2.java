package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.state.util.StateJsonConverter;

import java.util.LinkedHashMap;

@MigrationTransformation(targetVersionLabel = "0.2")
public class JoltTransformV1toV2 extends JoltTransformation {

	public JoltTransformV1toV2() {
		super(Version.V0_2);
	}


	@Override
	protected void initPostHooks() {
		postTransformHooks.add(JoltTransformV1toV2::sort);
	}

	@SuppressWarnings("unchecked")
	public static JsonNode sort (JsonNode node) throws MigrationException{
		LinkedHashMap source = (LinkedHashMap) StateJsonConverter.convertJsonNodeToObject(node);
		LinkedHashMap<Object, Object> sortedRoot = new LinkedHashMap<>();
		putObject(sortedRoot, source, "name");
		putObject(sortedRoot, source, "description");
		putObject(sortedRoot, source, "release");
		putObject(sortedRoot, source, "commithash");
		putObject(sortedRoot, source, "processWriters", "files", "processors", "isTimestamped");
		putObject(sortedRoot, source, "scenario", "mainModel", "attributesModel", "attributesSimulation", "eventInfos", "topography");

		return  StateJsonConverter.deserializeToNode(sortedRoot);
	}


}
