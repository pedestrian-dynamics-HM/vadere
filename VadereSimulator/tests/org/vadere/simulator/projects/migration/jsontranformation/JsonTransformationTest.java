package org.vadere.simulator.projects.migration.jsontranformation;

import org.junit.After;
import org.junit.Before;
import org.vadere.simulator.utils.reflection.TestJsonNodeExplorer;
import org.vadere.simulator.utils.reflection.TestJsonNodeHelper;
import org.vadere.simulator.utils.reflection.TestResourceHandlerScenario;

public abstract class JsonTransformationTest implements TestJsonNodeExplorer, TestJsonNodeHelper, TestResourceHandlerScenario {

	protected org.vadere.simulator.projects.migration.jsontranformation.JsonTransformationFactory factory = org.vadere.simulator.projects.migration.jsontranformation.JsonTransformationFactory.instance();


	@Before
	public void init() {
		backupTestDir();
	}

	@After
	public void cleaUp() {
		loadFromBackup();
	}


}