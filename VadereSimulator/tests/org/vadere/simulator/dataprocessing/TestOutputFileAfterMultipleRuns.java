package org.vadere.simulator.dataprocessing;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.MainModelBuilder;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.util.io.IOUtils;

public class TestOutputFileAfterMultipleRuns {

	private Scenario testScenario;
	private MainModel mainModel;

	@Before
	public void setup() {
		try {
			String json = IOUtils
					.readTextFile(getClass().getResource("/data/basic_1_chicken_osm1.scenario").getPath());
			testScenario = JsonConverter.deserializeScenarioRunManager(json);
			MainModelBuilder modelBuilder = new MainModelBuilder(testScenario.getScenarioStore());
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
				.createProcessorManager(mainModel);
		manager.initOutputFiles();
		List<OutputFile<?>> outputFiles = testScenario.getDataProcessingJsonManager().getOutputFiles();
		outputFiles.forEach(f -> headerAfterFirstRun.add(f.getHeader()));

		manager.initOutputFiles();
		outputFiles.forEach(f -> headerAfterSecondRun.add(f.getHeader()));

		assertEquals("Duplicated Processors in OutputFile after multiple Simulations",
				headerAfterFirstRun, headerAfterSecondRun);
	}
}
