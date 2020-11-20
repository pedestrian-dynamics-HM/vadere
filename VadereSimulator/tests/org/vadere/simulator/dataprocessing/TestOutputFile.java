package org.vadere.simulator.dataprocessing;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.MainModelBuilder;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.state.scenario.Topography;
import org.vadere.util.io.IOUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestOutputFile {

	private Scenario testScenario;
	private MainModel mainModel;
	private Topography topography;

	@Before
	public void setup() {
		try {
			String json = IOUtils
					.readTextFile(new File(getClass().getResource("/data/basic_1_chicken_osm1.scenario").toURI()).getAbsolutePath());
			testScenario = JsonConverter.deserializeScenarioRunManager(json);
			topography = testScenario.getTopography();
			MainModelBuilder modelBuilder = new MainModelBuilder(testScenario.getScenarioStore(), null, null);
			modelBuilder.createModelAndRandom();
			mainModel = modelBuilder.getModel();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * If a scenario is run multiple times each processor should only be present once.
	 */
	@Test()
	public void testFileFormatAfterMultipleSimulationRuns() {

		ArrayList<String> headerAfterFirstRun = new ArrayList<>();
		ArrayList<String> headerAfterSecondRun = new ArrayList<>();

		ProcessorManager manager = testScenario.getDataProcessingJsonManager()
				.createProcessorManager(mainModel, topography);
		manager.initOutputFiles();
		List<OutputFile<?>> outputFiles = testScenario.getDataProcessingJsonManager().getOutputFiles();
		outputFiles.forEach(f -> headerAfterFirstRun.add(f.getHeaderLine()));

		manager.initOutputFiles();
		outputFiles.forEach(f -> headerAfterSecondRun.add(f.getHeaderLine()));

		assertEquals("Duplicated Processors in OutputFile after multiple Simulations",
				headerAfterFirstRun, headerAfterSecondRun);
	}

	@Test
	public void testHandlingNameConflict(){
		ProcessorManager manager = testScenario.getDataProcessingJsonManager()
				.createProcessorManager(mainModel, topography);
		manager.initOutputFiles();

		List<String> header = testScenario.getDataProcessingJsonManager().getOutputFiles().get(0).getEntireHeader();

		assertTrue(header.contains("timeStep"));
		assertTrue(header.contains("pedestrianId"));

		assertTrue(header.contains(OutputFile.addHeaderProcInfo("x", 1)));
		assertTrue(header.contains(OutputFile.addHeaderProcInfo("y", 1)));
		assertTrue(header.contains(OutputFile.addHeaderProcInfo("x", 2)));
		assertTrue(header.contains(OutputFile.addHeaderProcInfo("y", 2)));
	}


}
