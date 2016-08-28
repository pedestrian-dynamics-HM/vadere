//package org.vadere.gui.vadere;
//
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.vadere.simulator.projects.ScenarioRunManager;
//import org.vadere.simulator.projects.dataprocessing.writer.ProcessorWriter;
//import org.vadere.simulator.projects.io.JsonConverter;
//import org.vadere.state.attributes.processors.AttributesWriter;
//
//import java.io.IOException;
//import java.util.LinkedList;
//
//import static org.junit.Assert.assertEquals;
//
//public class TestVadereTest {
//
//	private static ScenarioRunManager testInstance;
//	private static String testName = "testname";
//
//	@BeforeClass
//	public static void setUpBeforeClass() throws Exception {
//		testInstance = new ScenarioRunManager(testName);
//		testInstance.addWriter(new ProcessorWriter(
//				new MeanEvacuationTimeProcessor(new PedestrianLastPositionProcessor()), new AttributesWriter()));
//	}
//
//	@Test
//	public void testToJSON() throws IOException {
//		String json =
//				JsonConverter.serializeJsonNode(JsonConverter.serializeScenarioRunManagerToNode(testInstance, true));
//		ScenarioRunManager copy = JsonConverter.deserializeScenarioRunManager(json);
//
//		assertEquals(testInstance.getName(), copy.getName());
//		assertEquals(testInstance.getAllWriters(), copy.getAllWriters());
//
//		String jsonCopy = JsonConverter.serializeJsonNode(JsonConverter.serializeScenarioRunManagerToNode(copy, true));
//
//		assertEquals(jsonCopy, json);
//	}
//
//	@Test
//	public void testFromJSON() throws IOException {
//		/*
//		 * String json = IOUtils.toPrettyPrintJson(IOVadere.toJson(testInstance));
//		 * ScenarioRunManager testInstanceNew = IOVadere.fromJson(json);
//		 *
//		 * assertEquals(IOVadere.toJson(testInstance), IOVadere.toJson(testInstanceNew));
//		 */
//	}
//
//	@Test
//	public void testGetAttributeFilePath() {
//		assertEquals(testName, testInstance.getName());
//	}
//
//	@Test
//	public void testGetName() throws IOException {
//		assertEquals(testName, testInstance.getName());
//	}
//
//	@Test
//	public void testClone() throws IOException {
//		ScenarioRunManager clone = testInstance.clone();
//
//		// change the name of the clone and test if the old test kept its name
//		clone.setName("cloned test");
//		assertEquals(clone.getName(), "cloned test");
//		assertEquals(testInstance.getName(), testName);
//
//		// set output processors and check them
//		clone.setProcessWriters(new LinkedList<ProcessorWriter>());
//		assertEquals(0, clone.getAllWriters().size());
//		assertEquals(1, testInstance.getAllWriters().size());
//	}
//
//}
