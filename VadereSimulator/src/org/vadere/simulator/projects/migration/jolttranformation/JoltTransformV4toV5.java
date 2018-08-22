package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;

@MigrationTransformation(targetVersionLabel = "0.5")
public class JoltTransformV4toV5 extends JoltTransformation {

	public JoltTransformV4toV5() {
		super(Version.V0_4);
	}

	@Override
	protected void initPostHooks() {
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
