package org.vadere.simulator.projects.migration.jsontranformation;

import org.junit.After;
import org.junit.Before;
import org.vadere.simulator.utils.reflection.TestJsonNodeExplorer;
import org.vadere.simulator.utils.reflection.TestJsonNodeHelper;
import org.vadere.simulator.utils.reflection.TestResourceHandler;
import org.vadere.simulator.projects.migration.jsontranformation.JsonTransformationFactory;

abstract class JoltTransformationTest implements TestJsonNodeExplorer, TestJsonNodeHelper, TestResourceHandler {

	JsonTransformationFactory factory = JsonTransformationFactory.instance();


	@Before
	public void init() {
		backupTestDir();
	}

	@After
	public void cleaUp() {
		loadFromBackup();
	}


}