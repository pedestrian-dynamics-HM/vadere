package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;

public class JoltTransformV4toV5 extends JoltTransformation {

	public JoltTransformV4toV5(String transformation, String identity, Version version) throws MigrationException {
		super(transformation, identity, version);
	}

	@Override
	protected void initPostHooks() throws MigrationException {
		postTransformHooks.add(this::cleanupPedestrianOverlapProcessorAttribute);
		postTransformHooks.add(this::addOverlapProcessors);
		postTransformHooks.add(JoltTransformV1toV2::sort);
	}

	private JsonNode cleanupPedestrianOverlapProcessorAttribute(JsonNode node) throws MigrationException{
		return node;
	}

	private JsonNode addOverlapProcessors(JsonNode node) throws MigrationException{
		return node;
	}


}
