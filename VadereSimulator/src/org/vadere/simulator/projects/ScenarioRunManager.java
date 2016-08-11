package org.vadere.simulator.projects;

import difflib.DiffUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.PassiveCallback;
import org.vadere.simulator.control.Simulation;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.ModelBuilder;
import org.vadere.simulator.projects.dataprocessing.processors.ModelTest;
import org.vadere.simulator.projects.dataprocessing.processors.PedestrianPositionProcessor;
import org.vadere.simulator.projects.dataprocessing.processors.SnapshotOutputProcessor;
import org.vadere.simulator.projects.dataprocessing.writer.ProcessorWriter;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.attributes.processors.AttributesPedestrianPositionProcessor;
import org.vadere.state.attributes.processors.AttributesWriter;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.io.IOUtils;
import org.vadere.util.reflection.VadereClassNotFoundException;

/**
 * Receives an object of type ScenarioStore, manages a scenario and runs the simulation.
 * 
 */
public class ScenarioRunManager implements Runnable {

	private static Logger logger = LogManager.getLogger(ScenarioRunManager.class);

	protected ScenarioStore scenarioStore;
	protected Path outputPath;
	private Path processorOutputPath;

	private List<ProcessorWriter> processorWriters;
	private List<ModelTest> modelTests;
	protected final List<PassiveCallback> passiveCallbacks;
	protected List<ProcessorWriter> writers;

	private ScenarioFinishedListener finishedListener;
	protected Simulation simulation;
	private boolean scenarioFailed = false;
	private boolean simpleOutputProcessorName = false;

	private String savedStateSerialized;

	public ScenarioRunManager(final String name) {
		this(name, new ScenarioStore(name));
	}

	public ScenarioRunManager(final ScenarioStore store) {
		this(store.name, store);
	}

	public ScenarioRunManager(final String name, final ScenarioStore store) {
		this.passiveCallbacks = new LinkedList<>();
		this.processorWriters = new LinkedList<>();
		this.modelTests = new LinkedList<>();
		this.writers = new LinkedList<>();
		this.scenarioStore = store;
		this.outputPath = Paths.get(IOUtils.OUTPUT_DIR); // TODO [priority=high] [task=bugfix] [Error?] this is a relative path. If you start the application via eclipse this will be VadereParent/output
		this.processorOutputPath = Paths.get(IOUtils.OUTPUTPROCESSOR_OUTPUT_DIR);
		saveChanges();
	}

	public void saveChanges() { // get's called by VadereProject.saveChanges on init
		savedStateSerialized = JsonConverter.serializeScenarioRunManager(this);
	}

	public boolean hasUnsavedChanges() {
		return !savedStateSerialized.equals(JsonConverter.serializeScenarioRunManager(this));
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

			ModelBuilder modelBuilder = new ModelBuilder(scenarioStore);
			modelBuilder.createModelAndRandom();
			final MainModel mainModel = modelBuilder.getModel();
			final Random random = modelBuilder.getRandom();

			// Run simulation main loop from start time = 0 seconds
			simulation = new Simulation(mainModel, 0, scenarioStore.name, scenarioStore, passiveCallbacks, writers, random);
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

	public void addWriter(final ProcessorWriter writer) {
		processorWriters.add(writer);
		if (writer.getProcessor() instanceof ModelTest)
			modelTests.add((ModelTest) writer.getProcessor());
	}

	public void removeWriter(final ProcessorWriter writer) {
		processorWriters.remove(writer);
		if (writer instanceof ModelTest)
			modelTests.remove(writer.getProcessor());
	}

	public void removeAllWriters() {
		processorWriters.clear();
		modelTests.clear();
	}

	public void setProcessWriters(final List<ProcessorWriter> writers) {
		this.processorWriters.clear();
		for (ProcessorWriter writer : writers)
			addWriter(writer);
	}

	public List<ProcessorWriter> getAllWriters() {
		return this.processorWriters;
	}

	public void setOutputPaths(final Path outputPath, final Path processedOutputPath) {
		this.outputPath = outputPath;
		this.processorOutputPath = processedOutputPath;
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
		if (getAttributesSimulation().isWriteSimulationData()) {
			DateFormat format = new SimpleDateFormat(IOUtils.DATE_FORMAT);

			writers.clear();
			int writerCounter = 0; // needed to distinguish writers with the same name
			String dateString = format.format(new Date());
			String dirName = String.format("%s_%s", this.getName(), dateString);

			ProcessorWriter snapshotWriter = new ProcessorWriter(new SnapshotOutputProcessor(), new AttributesWriter());
			ProcessorWriter trajectoryWriter = new ProcessorWriter(
					new PedestrianPositionProcessor(new AttributesPedestrianPositionProcessor(true)),
					new AttributesWriter());
			String snapshotFileName =
					String.format("%s%s", getName(), snapshotWriter.getProcessor().getFileExtension());
			snapshotFileName = IOUtils.getPath(outputPath.resolve(dirName).toString(), snapshotFileName).toString();
			String trajectoyFileName =
					String.format("%s%s", getName(), trajectoryWriter.getProcessor().getFileExtension());
			trajectoyFileName = IOUtils.getPath(outputPath.resolve(dirName).toString(), trajectoyFileName).toString();
			try {
				snapshotWriter.setOutputStream(new FileOutputStream(snapshotFileName, false));
				snapshotWriter.setWriteHeader(false);
				trajectoryWriter.setOutputStream(new FileOutputStream(trajectoyFileName, false));
				trajectoryWriter.setWriteHeader(true);
				writers.add(snapshotWriter);
				writers.add(trajectoryWriter);
			} catch (IOException e) {
				logger.error(e);
			}

			for (ProcessorWriter writer : processorWriters) {
				Path processorOutputPath = null;
				processorOutputPath = this.processorOutputPath;

				String filename;
				if (simpleOutputProcessorName) {
					filename = String.format("%s%s", this.getName(), writer.getProcessor().getFileExtension());
				} else {
					filename = String.format("%s_%s_%d_%s%s", this.getName(), writer.getProcessor().getName(),
							(writerCounter++), dateString, writer.getProcessor().getFileExtension());
				}
				String procFileName = IOUtils.getPath(processorOutputPath.toString(), filename).toString();
				try {
					writer.setOutputStream(new FileOutputStream(procFileName, false));
				} catch (FileNotFoundException e) {
					logger.error(e);
				}
				writers.add(writer);
			}
		}
	}

	@Override
	public ScenarioRunManager clone() {
		ScenarioRunManager clonedScenario = null;
		try {
			clonedScenario = JsonConverter.cloneScenarioRunManager(this);
			clonedScenario.outputPath = outputPath;
			clonedScenario.processorOutputPath = processorOutputPath;
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

	public void discardChanges() { // is this done properly this way? Could replace the whole ScenarioRunManager in VadereProject (but keep the Id) alternatively
		try {
			ScenarioRunManager srm = JsonConverter.deserializeScenarioRunManager(savedStateSerialized);
			this.processorWriters = srm.processorWriters;
			this.writers = srm.writers;
			this.scenarioStore = srm.scenarioStore;
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
}
