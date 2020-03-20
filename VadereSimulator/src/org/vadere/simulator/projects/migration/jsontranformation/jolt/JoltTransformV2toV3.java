package org.vadere.simulator.projects.migration.jsontranformation.jolt;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.jsontranformation.JoltTransformation;

@MigrationTransformation(targetVersionLabel = "0.3")
public class JoltTransformV2toV3 extends JoltTransformation {
	public JoltTransformV2toV3() {
		super(Version.V0_3);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookLast(this::sort);
	}


}
