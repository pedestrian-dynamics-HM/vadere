package org.vadere.simulator.projects.migration.jolttranformation;

import org.vadere.simulator.entrypoints.Version;

public class JoltTransformV2toV3 extends JoltTransformation {
	public JoltTransformV2toV3(String transformation, String identity, Version version) {
		super(transformation, identity, version);
	}

	@Override
	protected void initPostHooks() {

	}
}
