package org.vadere.gui.vadere;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.io.JsonConverter;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;


public class TestVadereTestProject {

	@Test
	public void testToJson() throws IOException {
		final String scenarioJson = loadTestScenarioJson();
		final Scenario srm = JsonConverter.deserializeScenarioRunManager(scenarioJson);
		
		final String serializedJson = JsonConverter.serializeScenarioRunManager(srm);
		// TODO implement json test
	}

	@Test
	public void testFromJson() throws IOException {
		final String scenarioJson = loadTestScenarioJson();
		final Scenario srm = JsonConverter.deserializeScenarioRunManager(scenarioJson);

		assertEquals("Neues_Szenario", srm.getName());
		assertEquals(3, srm.getModelAttributes().size());
		assertEquals(21, srm.getDataProcessingJsonManager().getDataProcessors().size());
		assertEquals(7, srm.getDataProcessingJsonManager().getOutputFiles().size());
		assertTrue(srm.getAttributesPedestrian() != null);
		assertTrue(srm.getAttributesSimulation() != null);
		assertTrue(srm.getTopography() != null);
	}

	private String loadTestScenarioJson() {
		try {
			final InputStream resourceAsStream = getClass().getResourceAsStream("/test-scenario.scenario");
			return IOUtils.toString(resourceAsStream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
