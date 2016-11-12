package org.vadere.simulator.projects;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.PassiveCallback;
import org.vadere.simulator.control.Simulation;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.MainModelBuilder;
import org.vadere.simulator.projects.dataprocessing.DataProcessingJsonManager;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.io.IOUtils;

/**
 * Receives an object of type ScenarioStore, manages a scenario and runs the simulation.
 * 
 * @author Jakob Schöttl
 * 
 */
public class ScenarioRun implements Runnable {

	private static Logger logger = LogManager.getLogger(ScenarioRun.class);

	private ScenarioStore scenarioStore;
	private Path outputPath;

	private final List<PassiveCallback> passiveCallbacks;

	private DataProcessingJsonManager dataProcessingJsonManager;
	private ProcessorManager processorManager;

	private ScenarioFinishedListener finishedListener;
	private Simulation simulation;

	private Scenario scenario; // TODO make final


	// TODO remove
	@Deprecated
	public ScenarioRun(final String name) {
		this(name, new ScenarioStore(name));
	}

	// TODO remove
	@Deprecated
	public ScenarioRun(final ScenarioStore store) {
		this(store.name, store);
	}

	// TODO remove
	@Deprecated
	public ScenarioRun(final String name, final ScenarioStore store) {
		this.passiveCallbacks = new LinkedList<>();
		this.scenarioStore = store;
		
		this.scenario = null;

		this.dataProcessingJsonManager = new DataProcessingJsonManager();
		this.setOutputPaths(Paths.get(IOUtils.OUTPUT_DIR)); // TODO [priority=high] [task=bugfix] [Error?] this is a relative path. If you start the application via eclipse this will be VadereParent/output
	}
	
	public ScenarioRun(final Scenario scenario) {
		this(scenario.getName(), scenario.getScenarioStore());
		this.scenario = scenario;
	}

	/**
	 * This method runs a simulation. It must not catch any exceptions! The
	 * caller (i.e. the calling thread) should catch exceptions and call
	 * {@link #simulationFailed(Throwable)}.
	 */
	@Override
	public void run() {
		try {
			logger.info(String.format("Initializing scenario. Start of scenario '%s'...", scenario.getName()));

			if (finishedListener != null)
				this.finishedListener.scenarioStarted(scenario);

			scenarioStore.topography.reset();

			MainModelBuilder modelBuilder = new MainModelBuilder(scenarioStore);
			modelBuilder.createModelAndRandom();

			final MainModel mainModel = modelBuilder.getModel();
			final Random random = modelBuilder.getRandom();
			
			// prepare processors and simulation data writer
			processorManager = dataProcessingJsonManager.createProcessorManager(mainModel);

			createAndSetOutputDirectory();

			scenario.saveToOutputPath(outputPath);

			scenario.sealAllAttributes();

			// Run simulation main loop from start time = 0 seconds
			simulation = new Simulation(mainModel, 0, scenarioStore.name, scenarioStore, passiveCallbacks, random, processorManager);
			simulation.run();

		} catch (Exception e) {
			throw new RuntimeException("Simulation failed.", e);
		} finally {
			doAfterSimulation();
		}
	}
	
	public void simulationFailed(Throwable e) {
			e.printStackTrace();
			logger.error(e);
			if (finishedListener != null)
				finishedListener.scenarioRunThrewException(scenario, e);
	}

	protected void doAfterSimulation() {
		if (finishedListener != null)
			finishedListener.scenarioFinished(scenario);

		passiveCallbacks.clear();

		logger.info(String.format("Simulation of scenario %s finished.", scenario.getName()));
	}

	// Getter...
	public boolean isRunning() {
		return simulation != null && simulation.isRunning();
	}

	public ScenarioStore getScenarioStore() {
		return scenarioStore;
	}

	public List<Attributes> getAttributesModel() {
		return scenarioStore.attributesList;
	}

	/**
	 * Returns a copy of the used ModelTypes in a natural order.
	 * This is useful for displaying the different Model Attributes in a good order.
	 * 
	 * @return the used ModelTypes in a natural order
	 */
	public List<Attributes> getSortedAttributesMode() {
		return new ArrayList<>(scenarioStore.attributesList);
	}

	public AttributesAgent getAttributesPedestrian() {
		return scenarioStore.topography.getAttributesPedestrian();
	}

	public AttributesSimulation getAttributesSimulation() {
		return scenarioStore.attributesSimulation;
	}

	public Topography getTopography() {
		return scenarioStore.topography;
	}

	public void addPassiveCallback(final PassiveCallback pc) {
		passiveCallbacks.add(pc);
	}

	public void setOutputPaths(final Path outputPath) {
		if (dataProcessingJsonManager.isTimestamped()) {
			String dateString = new SimpleDateFormat(IOUtils.DATE_FORMAT).format(new Date());
			this.outputPath = Paths.get(outputPath.toString(), String.format("%s_%s", scenario.getName(), dateString));
		} else {
			this.outputPath = Paths.get(outputPath.toString(), scenario.getName());
		}
	}

	public void setName(String name) {
		this.scenarioStore.name = name;
	}

	public void setAttributesModel(List<Attributes> attributesList) {
		scenarioStore.attributesList = attributesList;
	}

	public void setAttributesPedestrian(AttributesAgent attributesPedestrian) {
		scenarioStore.topography.setAttributesPedestrian(attributesPedestrian);
	}

	public void setAttributesSimulation(AttributesSimulation attributesSimulation) {
		this.scenarioStore.attributesSimulation = attributesSimulation;
	}

	public void setTopography(final Topography topography) {
		scenarioStore.topography = topography;
	}

	public void setScenarioFinishedListener(ScenarioFinishedListener finishedListener) {
		this.finishedListener = finishedListener;
	}

	public boolean pause() {
		if (simulation != null) {
			simulation.pause();
			return true;
		}
		return false;
	}

	public void resume() {
		if (simulation != null)
			simulation.resume();
	}


	// Output stuff...
	private void createAndSetOutputDirectory() {
		try {
			// Create output directory
			Files.createDirectories(outputPath);
			processorManager.setOutputPath(outputPath.toString());
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	public String toString() {
		return scenario.getName();
	}

	public String getDescription() {
		return scenarioStore.description;
	}

	public void setDescription(String description) {
		scenarioStore.description = description;
	}

	public String readyToRunResponse() { // TODO [priority=medium] [task=check] add more conditions
		if (scenarioStore.mainModel == null) {
			return scenarioStore.name + ": no mainModel is set";
		}
		return null;
	}

	public ProcessorManager getProcessorManager() {
		return this.processorManager;
	}

	public DataProcessingJsonManager getDataProcessingJsonManager() {
		return this.dataProcessingJsonManager;
	}

	public void setDataProcessingJsonManager(final DataProcessingJsonManager manager) {
		this.dataProcessingJsonManager = manager;
	}

	public Path getOutputPath() {
		return outputPath;
	}

	public Scenario getScenario() {
		return scenario;
	}
}
