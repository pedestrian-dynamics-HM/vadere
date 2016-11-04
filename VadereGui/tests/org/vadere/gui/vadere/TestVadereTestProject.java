package org.vadere.gui.vadere;

import org.junit.BeforeClass;
import org.junit.Test;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.VadereProject;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class TestVadereTestProject {

	private static VadereProject testProject;
	private static String testProjectName = "testprojectname";
	private static String testName = "testname";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		ConcurrentMap<String, ScenarioRunManager> tests = new ConcurrentHashMap<>();
		tests.put(testName + "1", new ScenarioRunManager(testName + "1"));
		tests.put(testName + "2", new ScenarioRunManager(testName + "2"));
		tests.put(testName + "3", new ScenarioRunManager(testName + "3"));
		// testProject = new VadereProject(testProjectName, tests); // TODO [priority=medium] [task=test] fix test
	}

	@Test
	public void testToJSON() throws IOException {
		// TODO implement json test
		/*
		 * String json = IOUtils.toPrettyPrintJson(IOVadere.toJson(testProject));
		 * // System.out.println(json);
		 * 
		 * Map<String, Object> store = fromJson(json, Map.class);
		 * 
		 * assertEquals(testProject.getName(), store.get("name"));
		 * 
		 * List<Map<Object, String>> tests = (List<Map<Object, String>>) store.get("scenarios");
		 * assertEquals(IOVadere.toJson(testProject.getScenario(0)),
		 * IOVadere.toJson(IOVadere.fromJson(IOUtils.toJson(tests.get(0)))));
		 * assertEquals(IOVadere.toJson(testProject.getScenario(1)),
		 * IOVadere.toJson(IOVadere.fromJson(IOUtils.toJson(tests.get(1)))));
		 * assertEquals(IOVadere.toJson(testProject.getScenario(2)),
		 * IOVadere.toJson(IOVadere.fromJson(IOUtils.toJson(tests.get(2)))));
		 */
	}

	@Test
	public void testFromJSON() throws IOException {
		// TODO implement json test
		/*
		 * String json = IOUtils.toPrettyPrintJson(IOVadere.toJson(testProject));
		 * VadereProject testInstanceNew = IOVadere.projectFromJson(json);
		 * 
		 * assertEquals(IOVadere.toJson(testProject), IOVadere.toJson(testInstanceNew));
		 */
	}
}
