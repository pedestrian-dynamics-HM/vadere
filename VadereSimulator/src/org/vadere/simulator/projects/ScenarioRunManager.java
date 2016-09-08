package org.vadere.simulator.projects;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.PassiveCallback;
import org.vadere.simulator.control.Simulation;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.ModelBuilder;
import org.vadere.simulator.projects.dataprocessing.ModelTest;
import org.vadere.simulator.projects.dataprocessing.DataProcessingJsonManager;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.io.IOUtils;
import org.vadere.util.reflection.VadereClassNotFoundException;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import difflib.DiffUtils;

/**
 * Receives an object of type ScenarioStore, manages a scenario and runs the simulation.
 * 
 */
public class ScenarioRunManager implements Runnable {

	private static Logger logger = LogManager.getLogger(ScenarioRunManager.class);

	protected ScenarioStore scenarioStore;
	protected Path outputPath;

	private List<ModelTest> modelTests;
	protected final List<PassiveCallback> passiveCallbacks;

	private DataProcessingJsonManager dataProcessingJsonManager;
	protected ProcessorManager processorManager;

	private ScenarioFinishedListener finishedListener;
	protected Simulation simulation;
	private boolean scenarioFailed = false;
	private boolean simpleOutputProcessorName = false;

	private String savedStateSerialized;
	private String currentStateSerialized;


	public ScenarioRunManager(final String name) {
		this(name, new ScenarioStore(name));
	}

	public ScenarioRunManager(final ScenarioStore store) {
		this(store.name, store);
	}

	public ScenarioRunManager(final String name, final ScenarioStore store) {
		this.passiveCallbacks = new LinkedList<>();
		this.modelTests = new LinkedList<>();
		this.scenarioStore = store;

		this.dataProcessingJsonManager = new DataProcessingJsonManager();
		this.setOutputPaths(Paths.get(IOUtils.OUTPUT_DIR)); // TODO [priority=high] [task=bugfix] [Error?] this is a relative path. If you start the application via eclipse this will be VadereParent/output

		this.saveChanges();
	}

	public void saveChanges() { // get's called by VadereProject.saveChanges on init
		savedStateSerialized = JsonConverter.serializeScenarioRunManager(this);
		currentStateSerialized = savedStateSerialized;
	}

	public boolean hasUnsavedChanges() {
		return !savedStateSerialized.equals(currentStateSerialized);
	}

	public void updateCurrentStateSerialized() {
		currentStateSerialized = JsonConverter.serializeScenarioRunManager(this);
	}

	public String getDiff() {
		String currentStateSerialized = JsonConverter.serializeScenarioRunManager(this);
		if (!savedStateSerialized.equals(currentStateSerialized)) {
			StringBuilder diff = new StringBuilder();
			List<String> original = new ArrayList<>(Arrays.asList(savedStateSerialized.split("\n")));
			List<String> revised = new ArrayList<>(Arrays.asList(currentStateSerialized.split("\n")));
			DiffUtils.diff(original, revised).getDeltas().forEach(delta -> {
				String orig = delta.getOriginal().getLines().toString();
				String rev = delta.getRevised().getLines().toString();
				if (!orig.contains("hash") && !rev.contains(("hash"))) // TODO [priority=medium] [task=check] is "hash" a solid enough identifier? might checking orig be enough?
					diff.append("\n- line " + delta.getOriginal().getPosition() + ": " + orig + " to " + rev);
			});
			return diff.toString();
		}
		return null;
	}

	@Override
	public void run() {
		doBeforeSimulation();

		try {
			// prepare processors and simulation data writer
			prepareOutput();

			try (PrintWriter out = new PrintWriter(Paths.get(this.outputPath.toString(), this.getName() + IOUtils.SCENARIO_FILE_EXTENSION).toString())) {
				out.println(JsonConverter.serializeScenarioRunManager(this, true));
			}

			ModelBuilder modelBuilder = new ModelBuilder(scenarioStore);
			modelBuilder.createModelAndRandom();
			final MainModel mainModel = modelBuilder.getModel();
			final Random random = modelBuilder.getRandom();

			// Run simulation main loop from start time = 0 seconds
			simulation = new Simulation(mainModel, 0, scenarioStore.name, scenarioStore, passiveCallbacks, random, processorManager);
			simulation.run();
		} catch (Exception e) {
			scenarioFailed = true;
			if (finishedListener != null)
				this.finishedListener.scenarioRunThrewException(this, new Throwable(e));
			e.printStackTrace();
			logger.error(e);
		} finally {
			doAfterSimulation();
		}
	}

	protected void doAfterSimulation() {
		if (finishedListener != null)
			this.finishedListener.scenarioFinished(this);

		this.passiveCallbacks.clear();

		logger.info(String.format("Scenario finished."));
		logger.info(String.format("Running output processors, if any..."));
		logger.info(String.format("Done running scenario '%s': '%s'", this.getName(),
				(isSuccessful() ? "SUCCESSFUL" : "FAILURE")));
	}

	protected void doBeforeSimulation() {
		if (finishedListener != null)
			this.finishedListener.scenarioStarted(this);

		logger.info(String.format("Initializing scenario. Start of scenario '%s'...", this.getName()));
		scenarioStore.topography.reset();

		this.processorManager = this.dataProcessingJsonManager.createProcessorManager();
	}

	public void setScenarioFailed(final boolean scenarioFailed) {
		this.scenarioFailed = scenarioFailed;
	}

	// Getter...
	public boolean isRunning() {
		return simulation != null && simulation.isRunning();
	}

	public boolean isScenarioFailed() {
		return scenarioFailed;
	}

	public String getName() {
		return scenarioStore.name;
	}

	public ScenarioStore getScenarioStore() {
		return this.scenarioStore;
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
		this.passiveCallbacks.add(pc);
	}

	public void setOutputPaths(final Path outputPath) {
		String dateString = new SimpleDateFormat(IOUtils.DATE_FORMAT).format(new Date());
		this.outputPath = Paths.get(outputPath.toString(), String.format("%s_%s", this.getName(), dateString));
	}

	public void setName(String name) {
		this.scenarioStore.name = name;
	}

	public boolean isSuccessful() {
		for (ModelTest modelTest : modelTests) {
			if (!modelTest.isSucceeded()) {
				return false;
			}
		}
		return true;
	}

	public void setAttributesModel(List<Attributes> attributesList) {
		this.scenarioStore.attributesList = attributesList;
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
	private void prepareOutput() {
		this.processorManager.setOutputPath(this.outputPath.toString());
	}

	@Override
	public ScenarioRunManager clone() {
		ScenarioRunManager clonedScenario = null;
		try {
			clonedScenario = JsonConverter.cloneScenarioRunManager(this);
			clonedScenario.outputPath = outputPath;
		} catch (IOException | VadereClassNotFoundException e) {
			logger.error(e);
		}
		return clonedScenario;
	}

	@Override
	public String toString() {
		return getName();
	}

	public void setSimpleOutputProcessorName(boolean simpleOutputProcessorName) {
		this.simpleOutputProcessorName = simpleOutputProcessorName;
	}

	public String getDisplayName() {
		return scenarioStore.name + (hasUnsavedChanges() ? "*" : "");
	}

	public void discardChanges() {
		try {
			ScenarioRunManager srm = JsonConverter.deserializeScenarioRunManager(savedStateSerialized);
			// not all necessary! only the ones that could have changed
			this.scenarioStore = srm.scenarioStore;
			this.outputPath = srm.outputPath;
			this.processorManager = srm.processorManager;
			this.modelTests = srm.modelTests;
			this.finishedListener = srm.finishedListener;
			this.simulation = srm.simulation;
			this.scenarioFailed = srm.scenarioFailed;
			this.simpleOutputProcessorName = srm.simpleOutputProcessorName;
			//this.passiveCallbacks = srm.passiveCallbacks; // is final, can't be reassigned
		} catch (IOException | VadereClassNotFoundException e) {
			e.printStackTrace();
		}
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
}
