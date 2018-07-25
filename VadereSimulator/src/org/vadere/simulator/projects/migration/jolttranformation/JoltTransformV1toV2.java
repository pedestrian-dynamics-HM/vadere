package org.vadere.simulator.projects.migration.jolttranformation;

import org.vadere.simulator.entrypoints.Version;

public class JoltTransformV1toV2 extends JoltTransformation {

	public JoltTransformV1toV2(String transformation, String identity, Version version) {
		super(transformation, identity, version);
	}

	@Override
	protected void initPostHooks() {

	}
}
