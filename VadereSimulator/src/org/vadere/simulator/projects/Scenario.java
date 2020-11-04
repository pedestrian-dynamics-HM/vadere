package org.vadere.simulator.projects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.simulator.projects.dataprocessing.DataProcessingJsonManager;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesPsychology;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;
import org.vadere.util.reflection.VadereClassNotFoundException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import difflib.DiffUtils;

/**
 * Represents a Vadere scenario.
 * Holds a {@link ScenarioStore} object.
 * 
 * @author Jakob Schöttl
 * 
 */
public class Scenario {

	private static Logger logger = Logger.getLogger(Scenario.class);
	private ScenarioStore scenarioStore;
	private ScenarioStore simulationScenarioStore;
	private DataProcessingJsonManager dataProcessingJsonManager;
	private String savedStateSerialized;
	private String currentStateSerialized;
	private boolean simulationRunning;	// manage which copy of ScenarioStore is currently used.


	public Scenario(final String name) {
		this(new ScenarioStore(name));
	}

	public Scenario(@NotNull final ScenarioStore store) {
		this.scenarioStore = store;
		this.simulationRunning = false;
		this.dataProcessingJsonManager = new DataProcessingJsonManager();
		this.saveChanges();
	}

	public boolean isSimulationRunning() {
		return simulationRunning;
	}

	/**
	 * Creates a copy {@link ScenarioStore} which will be used in the simulation. After the simulation
	 * finishes, this copy is removed and the base version is used again. This is necessary, because
	 * the simulation seed is calculated for each simulation run and thus would change the base
	 * version of the simulation. The newly generated seed must be saved in the output copy of the
	 * {@link Scenario} file but not in the base version of the Scenario file.
	 */
	public void setSimulationRunning(boolean simulationRunning) {
		if (simulationRunning){
			this.simulationScenarioStore = copyScenarioStore();
		} else {
			this.simulationScenarioStore = null;
		}
		this.simulationRunning = simulationRunning;
	}

	public ScenarioStore copyScenarioStore() {
		try {
			return JsonConverter.cloneScenarioStore(this.scenarioStore);
		} catch (IOException e) {
			throw  new RuntimeException();
		}
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

	public String getName() {
		return scenarioStore.getName();
	}

	public ScenarioStore getScenarioStore() {
		return simulationRunning ? simulationScenarioStore : scenarioStore;
	}

	public List<Attributes> getModelAttributes() {
		return scenarioStore.getAttributesList();
	}

	public AttributesAgent getAttributesPedestrian() {
		return scenarioStore.getTopography().getAttributesPedestrian();
	}

	public AttributesSimulation getAttributesSimulation() {
		return scenarioStore.getAttributesSimulation();
	}

	public AttributesPsychology getAttributesPsychology() {
		return scenarioStore.getAttributesPsychology();
	}

	public Topography getTopography() {
		return scenarioStore.getTopography();
	}

	public void setName(@NotNull final String name) {
		this.scenarioStore.setName(name);
	}

	public void setAttributesModel(@NotNull final List<Attributes> attributesList) {
		scenarioStore.setAttributesList(attributesList);
	}

	public void setAttributesSimulation(@NotNull final AttributesSimulation attributesSimulation) {
		this.scenarioStore.setAttributesSimulation(attributesSimulation);
	}

	public void setAttributesPsychology(@NotNull final AttributesPsychology attributesPsychology) {
		this.scenarioStore.setAttributesPsychology(attributesPsychology);
	}


	public void setTopography(@NotNull final Topography topography) {
		scenarioStore.setTopography(topography);
	}

	@Override
	public Scenario clone() {
		try {
			return JsonConverter.cloneScenarioRunManager(this);
		} catch (IOException | VadereClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return getName();
	}

	public String getDisplayName() {
		return scenarioStore.getName() + (hasUnsavedChanges() ? "*" : "");
	}

	public void discardChanges() {
		try {
			Scenario srm = JsonConverter.deserializeScenarioRunManager(savedStateSerialized);
			// not all necessary! only the ones that could have changed
			scenarioStore = srm.scenarioStore;
			dataProcessingJsonManager = srm.dataProcessingJsonManager;
		} catch (IOException | VadereClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public String getDescription() {
		return scenarioStore.getDescription();
	}

	public void setDescription(String description) {
		scenarioStore.setDescription(description);
	}

	public String readyToRunResponse() { // TODO [priority=medium] [task=check] add more conditions
		if (scenarioStore.getMainModel() == null) {
			return scenarioStore.getName() + ": no mainModel is set";
		}
		return null;
	}

	public DataProcessingJsonManager getDataProcessingJsonManager() {
		return dataProcessingJsonManager;
	}

	public void setDataProcessingJsonManager(@NotNull final DataProcessingJsonManager manager) {
		this.dataProcessingJsonManager = manager;
	}

	public void saveToOutputPath(@NotNull final Path outputPath) {
		try (PrintWriter out = new PrintWriter(Paths.get(outputPath.toString(), getName() + IOUtils.SCENARIO_FILE_EXTENSION).toString())) {
			out.println(JsonConverter.serializeScenarioRunManager(this, true));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
