package org.vadere.simulator.projects.migration.jsontranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.util.version.Version;

import org.junit.Test;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.incident.helper.JsonFilterIterator;

import java.nio.file.Path;
import java.util.Iterator;

import static org.junit.Assert.*;

public class JoltTransformV4toV5Test extends JsonTransformationTest {


	@Override
	public Path getTestDir() {
		return getPathFromResources("/migration/v04_to_v05");
	}

	private JsonNode test001() {
		String test001 = getTestFileAsString("test001.scenario");
		assertThat(test001, versionMatcher(Version.V0_4));
		return getJsonFromString(test001);
	}

	private JsonNode test002() {
		String test002 = getTestFileAsString("test002.scenario");
		assertThat(test002, versionMatcher(Version.V0_4));
		return getJsonFromString(test002);
	}

	@Test
	public void needsBoundaryIsRemoved() {
		JsonNode v4 = test001();
		pathMustExist(v4, "scenario/attributesSimulation/needsBoundary");
		JsonTransformation t = factory.getJoltTransformV4toV5();
		JsonNode v5 = null;
		try {
			v5 = t.applyAll(v4);
		} catch (MigrationException e) {
			fail("Should not fail with MigrationException: " + e.getMessage());
		}

		assertThat(pathMustExist(v5, "release"), nodeHasText(Version.V0_5.label()));
		pathLastElementMustNotExist(v5, "scenario/attributesSimulation/needsBoundary");
	}

	/**
	 * Test with scenario containing a PedestrianOverlapProcessor processor. In this case The
	 * Attributes have to be deleted.
	 */
	@Test
	public void PedestrianOverlapProcessorAttributeIsRemoved() {
		testAttribute(test001(), 4, 1);
	}

	/**
	 * Test with scenario not containing a PedestrianOverlapProcessor processor. In this case
	 * Nothing should happen.
	 */
	@Test
	public void NoPedestrianOverlapProcessor() {
		testAttribute(test002(), 5, 0);
	}

	private void testAttribute(JsonNode v4, int processorCount, int overlapProcessors) {
		JsonNode processors_v4 = pathMustExist(v4, "processWriters/processors");

		Iterator<JsonNode> iter_v4 = new JsonFilterIterator(processors_v4, n -> {
			String type = pathMustExist(n, "type").asText();
			return type.equals("org.vadere.simulator.projects.dataprocessing.processor.PedestrianOverlapProcessor");
		});
		assertEquals("There should be 4 processors", processorCount, processors_v4.size());
		int count = 0;
		while (iter_v4.hasNext()) {
			count++;
			JsonNode p = iter_v4.next();
			pathMustExist(p, "attributes");
			pathMustExist(p, "attributesType");
		}
		assertEquals("There should be 1 OverlapsProcessor", overlapProcessors, count);

		JoltTransformation t = factory.getJoltTransformV4toV5();
		JsonNode v5 = null;
		try {
			v5 = t.applyAll(v4);
		} catch (MigrationException e) {
			fail("Should not fail with MigrationException: " + e.getMessage());
		}

		JsonNode processors_v5 = pathMustExist(v5, "processWriters/processors");
		Iterator<JsonNode> iter_v5 = new JsonFilterIterator(processors_v5, n -> {
			String type = pathMustExist(n, "type").asText();
			return type.equals("org.vadere.simulator.projects.dataprocessing.processor.PedestrianOverlapProcessor");
		});
		assertEquals("There should be 4 processors", processorCount, processors_v5.size());
		count = 0;
		while (iter_v5.hasNext()) {
			count++;
			JsonNode p = iter_v5.next();
			pathMustNotExist(p, "attributes");
			pathMustNotExist(p, "attributesType");
		}
		assertEquals("There should be 1 OverlapsProcessor", overlapProcessors, count);

	}

}