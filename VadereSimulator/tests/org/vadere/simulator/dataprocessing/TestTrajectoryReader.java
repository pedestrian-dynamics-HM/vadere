package org.vadere.simulator.dataprocessing;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.simulator.projects.io.TestUtils;
import org.vadere.simulator.projects.io.TrajectoryReader;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.simulation.Step;
import org.vadere.tests.util.reflection.TestResourceHandler;
import org.vadere.util.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class TestTrajectoryReader implements TestResourceHandler {

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
		folderName = "Test1_2015-03-11_17-42-08.826";
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
		TestUtils.copyDirTo(dest, backup);
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

	@Test
	public void testTrajectoryReader() throws IOException {
		Path dir = project.getOutputDir().resolve(folderName);
		TrajectoryReader reader = new TrajectoryReader(
				IOUtils.getFirstFile(dir.toFile(), IOUtils.TRAJECTORY_FILE_EXTENSION).get().toPath(), test);
		Map<Step, List<Agent>> pedestriansByStep = reader.readFile();

		assertEquals("incorrect number of steps " + pedestriansByStep.size() + " != " + 120, 120, pedestriansByStep.size());
		List<Step> sortedSteps = pedestriansByStep.keySet().stream()
				.sorted((s1, s2) -> s1.getStepNumber() - s2.getStepNumber()).collect(Collectors.toList());
		assertEquals("incorrect number of steps", 120, sortedSteps.size());

		IntStream.range(0, sortedSteps.size())
				.forEach(i -> assertTrue("missing step " + i, sortedSteps.get(i).getStepNumber() == i + 1));

		assertEquals("wrong number of pedestrians", 5, pedestriansByStep.get(sortedSteps.get(10)).size());
		assertEquals("wrong number of pedestrians", 7, pedestriansByStep.get(sortedSteps.get(39)).size());
	}
}
