package org.vadere.simulator.dataprocessing;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.simulator.utils.reflection.TestResourceHandlerScenario;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.util.test.TestUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class TestTrajectoryReader implements TestResourceHandlerScenario {

	private Scenario test;
	private VadereProject project;
	private String folderName;

	@Override
	public Path getTestDir() {
		return getPathFromResources("/data/VTestMultiRun");
	}

	@Before
	public void setUp() throws URISyntaxException {
		resetTestStructure();
		folderName = "Test1_2019-09-23_17-25-51.21";
		AttributesAgent attributes = new AttributesAgent();
		try {
			project = IOVadere.readProjectJson(getRelativeTestPath("vadere.project").toString());
			test = project.getScenarios().stream().filter(t -> t.getName().equals("Test1")).findFirst().get();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void resetTestStructure() throws URISyntaxException {
		String dest = getPathFromResources("/data/VTestMultiRun").toString();
		String backup = getPathFromResources("/data/VTestMultiRun.bak").toString();
		TestUtils.copyDirTo(backup, dest);
	}


	@Test
	public void testFolderAvailable() {
		assertNotNull("Test directory missing", getClass().getResource("/data/VTestMultiRun"));
		assertNotNull("Test directory missing", getClass().getResource("/data/VTestMultiRun/output"));
		assertNotNull("Test directory missing", getClass().getResource("/data/VTestMultiRun/output/" + folderName));

		try {
			assertTrue("Test directory is not a directory",
					new File(getClass().getResource("/data/VTestMultiRun/output/" + folderName).toURI())
							.isDirectory());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
