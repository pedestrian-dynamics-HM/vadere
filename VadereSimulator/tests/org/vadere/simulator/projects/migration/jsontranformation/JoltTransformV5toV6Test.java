package org.vadere.simulator.projects.migration.jsontranformation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.junit.Test;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;

import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class JoltTransformV5toV6Test extends JsonTransformationTest {

	@Override
	public Path getTestDir() {
		return getPathFromResources("/migration/v05_to_v06");
	}

	private JsonNode test001() {
		String test001 = getTestFileAsString("test001.scenario");
		assertThat(test001, versionMatcher(Version.V0_5));
		return getJsonFromString(test001);
	}

	private JsonNode test002() {
		String test002 = getTestFileAsString("test002.scenario");
		assertThat(test002, versionMatcher(Version.V0_5));
		return getJsonFromString(test002);
	}

	@Test
	public void testTransformWithWrongKey(){
		JsonNode v5 = test001();
		JoltTransformation t = factory.getJoltTransformV5toV6();
		JsonNode v6 = null;
		try {
			v6 = t.applyAll(v5);
		} catch (MigrationException e) {
			fail("Should not fail with MigrationException: " + e.getMessage());
		}


		assertThat(pathMustExist(v6, "release"), nodeHasText(Version.V0_6.label()));
		ArrayList<JsonNode> processors = getProcessorsByType(v6,
				"org.vadere.simulator.projects.dataprocessing.processor.PedestrianOverlapProcessor");
		assertEquals("One Processor expected", 1, processors.size());
		String pId = pathMustExist(processors.get(0), "id").asText();

		ArrayList<JsonNode> files = getFilesForProcessorId(v6,pId);
		assertEquals("One File expected", 1, files.size());
		String type = pathMustExist(files.get(0), "type").asText();
		assertEquals("Must be TimestepPedestrianIdOverlapKey",
				"org.vadere.simulator.projects.dataprocessing.outputfile.TimestepPedestrianIdOverlapOutputFile", type);

		ArrayNode allFiles = (ArrayNode)pathMustExist(v6, "processWriters/files");
		assertEquals("Must be  Three OutputFiles", 3,allFiles.size());
	}

	@Test
	public void testTransformWithCorrectKey(){
		JsonNode v5 = test002();
		JsonTransformation t = factory.getJoltTransformV5toV6();
		JsonNode v6 = null;
		try {
			v6 = t.applyAll(v5);
		} catch (MigrationException e) {
			fail("Should not fail with MigrationException: " + e.getMessage());
		}


		assertThat(pathMustExist(v6, "release"), nodeHasText(Version.V0_6.label()));
		ArrayList<JsonNode> processors = getProcessorsByType(v6,
				"org.vadere.simulator.projects.dataprocessing.processor.PedestrianOverlapProcessor");
		assertEquals("One Processor expected", 1, processors.size());
		String pId = pathMustExist(processors.get(0), "id").asText();

		ArrayList<JsonNode> files = getFilesForProcessorId(v6,pId);
		assertEquals("One File expected", 1, files.size());

		ArrayNode allFiles = (ArrayNode)pathMustExist(v6, "processWriters/files");
		assertEquals("Only Tow OutputFiles", 2,allFiles.size());

		ArrayList<JsonNode> pedestrianOverlapDistProcessorV5 = getProcessorsByType(v5,
				"org.vadere.simulator.projects.dataprocessing.processor.PedestrianOverlapDistProcessor");
		ArrayList<JsonNode> pedestrianOverlapDistProcessorV6 = getProcessorsByType(v6,
				"org.vadere.simulator.projects.dataprocessing.processor.PedestrianOverlapDistProcessor");
		assertEquals("Old vesion must contain PedestrianOverlapDistProcessor", 1, pedestrianOverlapDistProcessorV5.size());
		assertEquals("New version must not contain PedestrianOverlapDistProcessor", 0,pedestrianOverlapDistProcessorV6.size());
	}

}