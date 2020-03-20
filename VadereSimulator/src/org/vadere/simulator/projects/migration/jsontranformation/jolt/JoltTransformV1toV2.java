package org.vadere.simulator.projects.migration.jsontranformation.jolt;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.jsontranformation.JoltTransformation;

@MigrationTransformation(targetVersionLabel = "0.2")
public class JoltTransformV1toV2 extends JoltTransformation {

	public JoltTransformV1toV2() {
		super(Version.V0_2);
	}



	@Override
	protected void initDefaultHooks() {
        addPostHookLast(this::sort);
	}


}
