package org.vadere.gui.vadere;

import org.junit.BeforeClass;
import org.junit.Test;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.simulator.projects.ProjectWriter;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.models.AttributesPotentialCompact;
import org.vadere.state.attributes.models.AttributesPotentialOSM;
import org.vadere.state.scenario.Topography;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import static org.junit.Assert.assertEquals;

public class TestProjectWriterAndReader {

	private static VadereProject testProject;
	private static String testProjectName = "testprojectname";
	private static String testName = "testname";

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		LinkedList<Attributes> attributes = new LinkedList<>();
		attributes.add(new AttributesOSM());
		attributes.add(new AttributesPotentialCompact());
		attributes.add(new AttributesFloorField());
		attributes.add(new AttributesPotentialOSM());
		LinkedList<Scenario> tests = new LinkedList<>();
		tests.add(new Scenario(new ScenarioStore(testName + "1", "", OptimalStepsModel.class.getName(), attributes, new AttributesSimulation(), new Topography())));
		tests.add(new Scenario(new ScenarioStore(testName + "2", "", OptimalStepsModel.class.getName(), attributes, new AttributesSimulation(), new Topography())));
		tests.add(new Scenario(new ScenarioStore(testName + "3", "", OptimalStepsModel.class.getName(), attributes, new AttributesSimulation(), new Topography())));
		testProject = new VadereProject(testProjectName, tests, Paths.get("."));
	}

	/**
	 * Test method for
	 * {@link org.vadere.simulator.projects.io.IOVadere#readProject(java.lang.String)} .
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	@Test
	public void testWriteReadProject()
            throws IOException, ParserConfigurationException, SAXException, TransformerException, URISyntaxException {
		String testFileJson =
                new File(getClass().getResource("/input/testProjectFile/").toURI()).toString();
		ProjectWriter.writeProjectFileJson(testFileJson, testProject);
		VadereProject projectJson = IOVadere.readProjectJson(testFileJson);

		assertEquals(
				projectJson.getScenarios().stream().map(scenario -> JsonConverter.serializeScenarioRunManager(scenario))
						.collect(Collectors.toList()),
				testProject.getScenarios().stream().map(scenario -> JsonConverter.serializeScenarioRunManager(scenario))
						.collect(Collectors.toList()));
	}
}
