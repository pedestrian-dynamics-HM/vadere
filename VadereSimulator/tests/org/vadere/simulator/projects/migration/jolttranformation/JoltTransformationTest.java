package org.vadere.simulator.projects.migration.jolttranformation;

import org.junit.After;
import org.junit.Before;
import org.vadere.tests.util.reflection.JsonNodeTester;
import org.vadere.tests.util.reflection.TestResourceHandler;


abstract class JoltTransformationTest implements JsonNodeTester, TestResourceHandler {

	org.vadere.simulator.projects.migration.jolttranformation.JoltTransformationFactory factory = org.vadere.simulator.projects.migration.jolttranformation.JoltTransformationFactory.instance();


	@Before
	public void init() {
		backupTestDir();
	}

	@After
	public void cleaUp() {
		loadFromBackup();
	}


}