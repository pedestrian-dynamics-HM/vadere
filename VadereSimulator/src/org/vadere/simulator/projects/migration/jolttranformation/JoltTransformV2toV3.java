package org.vadere.simulator.projects.migration.jolttranformation;

import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;

public class JoltTransformV2toV3 extends JoltTransformation {
	public JoltTransformV2toV3(String transformation, String identity, Version version) throws MigrationException {
		super(transformation, identity, version);
	}

	@Override
	protected void initPostHooks() throws MigrationException {
//		postTransformHooks.add(this::postHook1);
	}

//	public JsonNode postHook1 (JsonNode node){
//		// implement
//		return  node;
//	}
}
