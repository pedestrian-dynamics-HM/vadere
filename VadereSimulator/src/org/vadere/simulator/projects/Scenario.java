package org.vadere.simulator.projects;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.dataprocessing.DataProcessingJsonManager;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.io.IOUtils;
import org.vadere.util.reflection.VadereClassNotFoundException;

import difflib.DiffUtils;

/**
 * Represents a Vadere scenario.
 * Holds a {@link ScenarioStore} object.
 * 
 * @author Jakob Schöttl
 * 
 */
public class Scenario {

	private static Logger logger = LogManager.getLogger(Scenario.class);

	private ScenarioStore scenarioStore;

	private DataProcessingJsonManager dataProcessingJsonManager;

	private String savedStateSerialized;
	private String currentStateSerialized;


	public Scenario(final String name) {
		this(name, new ScenarioStore(name));
	}

	public Scenario(final ScenarioStore store) {
		this(store.name, store);
	}

	public Scenario(final String name, final ScenarioStore store) {
		this.scenarioStore = store;

		this.dataProcessingJsonManager = new DataProcessingJsonManager();

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

	public String getName() {
		return scenarioStore.name;
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
		return scenarioStore.name + (hasUnsavedChanges() ? "*" : "");
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

	public DataProcessingJsonManager getDataProcessingJsonManager() {
		return dataProcessingJsonManager;
	}

	public void setDataProcessingJsonManager(final DataProcessingJsonManager manager) {
		this.dataProcessingJsonManager = manager;
	}

	public void saveToOutputPath(final Path outputPath) {
		try (PrintWriter out = new PrintWriter(Paths.get(outputPath.toString(), getName() + IOUtils.SCENARIO_FILE_EXTENSION).toString())) {
			out.println(JsonConverter.serializeScenarioRunManager(this, true));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
