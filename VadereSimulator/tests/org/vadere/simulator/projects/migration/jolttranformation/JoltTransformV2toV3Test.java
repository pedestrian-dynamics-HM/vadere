package org.vadere.simulator.projects.migration.jolttranformation;

import java.nio.file.Path;

public class JoltTransformV2toV3Test extends JoltTransformationTest{

	private final String TRANSFORM = "/transform_v0.2_to_v0.3.json";
	private final String IDENTITY = "/identity_v0.3.json";

	@Override
	protected Path getTestDir() {
		return null;
	}
}
