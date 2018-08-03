package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.state.util.StateJsonConverter;

import java.util.LinkedHashMap;

public class JoltTransformV1toV2 extends JoltTransformation {

	public JoltTransformV1toV2(String transformation, String identity, Version version) throws MigrationException {
		super(transformation, identity, version);
	}


	@Override
	protected void initPostHooks() {
		postTransformHooks.add(this::sort);
	}

	@SuppressWarnings("unchecked")
	public JsonNode sort (JsonNode node) throws MigrationException{
		LinkedHashMap source = (LinkedHashMap) StateJsonConverter.convertJsonNodeToObject(node);
		LinkedHashMap<Object, Object> sortedRoot = new LinkedHashMap<>();
		putObject(sortedRoot, source, "name");
		putObject(sortedRoot, source, "description");
		putObject(sortedRoot, source, "release");
		putObject(sortedRoot, source, "commithash");
		putObject(sortedRoot, source, "processWriters", "files", "processors", "isTimestamped");
		putObject(sortedRoot, source, "scenario", "mainModel", "attributesModel", "attributesSimulation", "topography");

		return  StateJsonConverter.deserializeToNode(sortedRoot);
	}


}
