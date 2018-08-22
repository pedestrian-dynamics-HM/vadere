package org.vadere.simulator.projects.migration.jolttranformation;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class JoltTransformV4toV5Test extends JoltTransformationTest {


	@Override
	protected Path getTestDir() {
		return getDirFromResources("/migration/v04_to_v05");
	}

}