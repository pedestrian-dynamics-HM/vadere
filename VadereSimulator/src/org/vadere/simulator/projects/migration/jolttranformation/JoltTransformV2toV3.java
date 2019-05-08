package org.vadere.simulator.projects.migration.jolttranformation;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.entrypoints.Version;

@MigrationTransformation(targetVersionLabel = "0.3")
public class JoltTransformV2toV3 extends JoltTransformation {
	public JoltTransformV2toV3() {
		super(Version.V0_3);
	}

	@Override
	protected void initPostHooks() {
		postTransformHooks.add(JoltTransformV1toV2::sort);
	}


}
