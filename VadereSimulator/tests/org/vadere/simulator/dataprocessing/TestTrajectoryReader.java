package org.vadere.simulator.dataprocessing;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.simulator.projects.io.TrajectoryReader;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.simulation.Step;
import org.vadere.util.io.IOUtils;
import org.vadere.util.reflection.VadereClassNotFoundException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class TestTrajectoryReader {

	private ScenarioRunManager test;
	private VadereProject project;
	private AttributesAgent attributes;
	private String folderName;
	private double EPSILON = 0.0000001;

	@Before
	public void setUp() {
		folderName = "Test1_2015-03-11_17-42-08.826";
		attributes = new AttributesAgent();
		try {
			project = IOVadere.readProjectJson(getClass().getResource("/data/VTestMultiRun/vadere.project").getPath().replaceFirst("^/(.:/)", "$1"));
			test = project.getScenarios().stream().filter(t -> t.getName().equals("Test1")).findFirst().get();

		} catch (ParserConfigurationException | SAXException | IOException | TransformerException
				| VadereClassNotFoundException e) {
			e.printStackTrace();
		}
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

		assertTrue("incorrect number of steps " + pedestriansByStep.size() + " != " + 120,
				pedestriansByStep.size() == 120);
		List<Step> sortedSteps = pedestriansByStep.keySet().stream()
				.sorted((s1, s2) -> s1.getStepNumber() - s2.getStepNumber()).collect(Collectors.toList());
		assertTrue("incorrect number of steps", sortedSteps.size() == 120);

		IntStream.range(0, sortedSteps.size())
				.forEach(i -> assertTrue("missing step " + i, sortedSteps.get(i).getStepNumber() == i + 1));

		assertTrue("wrong number of pedestrians", pedestriansByStep.get(sortedSteps.get(10)).size() == 5);
		assertTrue("wrong number of pedestrians", pedestriansByStep.get(sortedSteps.get(39)).size() == 7);
	}
}
