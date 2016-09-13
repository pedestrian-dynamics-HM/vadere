package org.vadere.simulator.entrypoints;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesBuilder;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.ScenarioElementBuilder;
import org.vadere.state.scenario.Teleporter;
import org.vadere.state.scenario.Topography;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * {@link VadereBuilder} provide methods to manipulate the attributes/parameters of a scenario.
 * It's an interface for generate multiple Vadere-Scenario based on the same infrastructure but
 * with different attributes/parameters. The class does not provide methods to change the
 * infrastructure of the {@link Topography}. To use this effectively the user need a
 * good knowledge about the parameters of the given scenario!
 * 
 *
 */
public class VadereBuilder {
	private static Logger logger = LogManager.getLogger(VadereBuilder.class);

	private ScenarioRunManager scenario;
	private ScenarioStore store;

	public VadereBuilder(final ScenarioRunManager base) {
		scenario = base.clone();
		store = scenario.getScenarioStore();
	}

	public ScenarioRunManager build() {
		store.topography = scenario.getTopography();
		return scenario;
	}

	/**
	 * Changes the name of the output-files that will be generated during and after the simulation.
	 * To prevent yourself from overriding files it's necessary to set a new output name for every
	 * generated scenario!
	 * 
	 * @param name the name of the output file
	 */
	public void setName(final String name) {
		scenario.setName(name);
	}

	// change attributes
	public <T> void setSimulationField(final String fieldName, final T value) {
		AttributesBuilder<AttributesSimulation> simAttsBuilder = new AttributesBuilder<>(store.attributesSimulation);
		simAttsBuilder.setField(fieldName, value);
		store.attributesSimulation = simAttsBuilder.build();
	}

	public <T> void setPedestrianField(final String fieldName, final T value) {
		/*
		 * AttributesBuilder<AttributesPedestrian> simAttsBuilder = new
		 * AttributesBuilder<>(store.attributesPedestrian);
		 * simAttsBuilder.setField(fieldName, value);
		 * store.attributesPedestrian = simAttsBuilder.build();
		 */
	}

	public <T> void setModel(final List<Attributes> model) {
		store.attributesList = model;
	}

	// change topography
	/**
	 * Assign the {@link value} to the field identified by {@link fieldName} of the first source,
	 * identified by {@link sourceId}.
	 * If no source was found, nothing will happen.
	 * 
	 * @param fieldName name of the field (e.g. spawnNumber)
	 * @param sourceId id of the source
	 * @param value value that will be assigned
	 */
	public <T> void setSourceField(final String fieldName, final int sourceId, final T value) {
		setField(scenario.getTopography().getSources(), sourceId, fieldName, value);
	}

	/**
	 * Assign the {@link value} to the field identified by {@link fieldName} of the first target,
	 * identified by {@link sourceId}.
	 * If no source was found, nothing will happen.
	 * 
	 * @param fieldName name of the field (e.g. spawnNumber)
	 * @param targetId id of the target
	 * @param value value that will be assigned
	 */
	public <T> void setTargetField(final String fieldName, final int targetId, final T value) {
		setField(scenario.getTopography().getTargets(), targetId, fieldName, value);
	}

	/**
	 * Assign the {@link value} to the field identified by {@link fieldName}.
	 * 
	 * @param fieldName field identifier
	 * @param value the assigned value
	 */
	public <T> void setTeleporterField(final String fieldName, final T value) {
		Teleporter teleporter = scenario.getTopography().getTeleporter();
		teleporter = setField(fieldName, teleporter, value);
		scenario.getTopography().setTeleporter(teleporter);
	}

	/**
	 * Assign the {@link value} to all the fields, identified by {@link fieldName}, of all sources.
	 * 
	 * @param fieldName field identifier
	 * @param value the assigned value
	 */
	public <T> void setAllSourcesField(final String fieldName, final T value) {
		setAllField(scenario.getTopography().getSources(), fieldName, value);
	}

	/**
	 * Assign the {@link value} to all the fields, identified by {@link fieldName}, of all targets.
	 * 
	 * @param fieldName field identifier
	 * @param value the assigned value
	 */
	public <T> void setAllTargetsField(final String fieldName, final T value) {
		setAllField(scenario.getTopography().getTargets(), fieldName, value);
	}

	/**
	 * Assign the i-th element of {@link values} to the field, identified by the {@link fieldName},
	 * of the i-th source.
	 * sources.get(i)[field] = values(i).
	 * 
	 * @param fieldName name of the attribute
	 * @param values array of values
	 */
	@SuppressWarnings("unchecked")
	public <T> void setMultipleSourcesField(final String fieldName, final T... values) {
		setFieldOfElements(fieldName, scenario.getTopography().getSources(), values);
	}

	/**
	 * Assign the i-th element of {@link values} to the field, identified by the {@link fieldName},
	 * of the i-th target.
	 * targets.get(i)[field] = values(i).
	 * 
	 * @param fieldName name of the attribute
	 * @param values array of values
	 */
	@SuppressWarnings("unchecked")
	public <T> void setMulitipleTargetsField(final String fieldName, final T... values) {
		setFieldOfElements(fieldName, scenario.getTopography().getTargets(), values);
	}


	// private helpers
	/**
	 * Sets the value of the field, identified by fieldName, of the i-th element in the list to the
	 * i-th value.
	 * list.get(i)[field] = values(i).
	 * 
	 * @param fieldName the name of the field that will be affected
	 * @param list list of elements that will be affected
	 * @param values values that correspond to the element.
	 */
	@SuppressWarnings("unchecked")
	private static <T, E extends ScenarioElement> void setFieldOfElements(final String fieldName, final List<E> list,
			final T... values) {
		int i = 0;
		while (i < values.length && i < list.size()) {
			E element = setField(fieldName, list.get(i), values[i]);
			list.remove(i);
			list.add(i, element);
			i++;
		}
	}

	private static <T, E extends ScenarioElement> void setAllField(final List<E> scenarioElements, final String name,
			final T value) {
		E tmp = null;
		for (int i = 0; i < scenarioElements.size(); i++) {
			tmp = scenarioElements.get(i);
			E element = setField(name, tmp, value);
			scenarioElements.remove(i);
			scenarioElements.add(i, element);
		}
	}

	/**
	 * Sets the field with the name=fieldName of element in scenarioElements, identified by his id,
	 * to value.
	 * If no element was found, nothing will happen. If there are more than one element with the id
	 * in the list,
	 * only the first one will be affected.
	 */
	private static <T, E extends ScenarioElement> void setField(final List<E> scenarioElements, final int id,
			final String name, final T value) {
		E tmp = null;
		for (int i = 0; i < scenarioElements.size(); i++) {
			tmp = scenarioElements.get(i);
			if (tmp.getId() == id) {
				E element = setField(name, tmp, value);
				scenarioElements.remove(i);
				scenarioElements.add(i, element);
				return;
			}
		}
		logger.warn("couldn't find scenarioElement (" + (tmp != null ? tmp.getType() : null) + ") with the id " + id);
	}

	/**
	 * Set's the field with the name=fieldName of element to value.
	 */
	private static <T, E extends ScenarioElement> E setField(final String fieldName, final E element, final T value) {
		ScenarioElementBuilder<E> elBuilder = new ScenarioElementBuilder<>(element);
		AttributesBuilder<Attributes> attBuilder = new AttributesBuilder<>(element.getAttributes());
		attBuilder.setField(fieldName, value);
		elBuilder.setAttributes(attBuilder.build());
		return elBuilder.build();
	}

	/**
	 * Search (depth first search) for an JsonObject that contains an JsonElement with the
	 * key.equals(fieldName).
	 * 
	 * @param fieldName the key we are searching for
	 * @param base the root of the search
	 * @return the JsonObject your searching for, or null if no element was found.
	 */
	private static JsonObject searchJsonElement(final String fieldName, JsonElement base) {
		if (base == null) {
			return null;
		} else if (base.isJsonObject()) {
			JsonObject jsonObject = (JsonObject) base;
			for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
				if (entry.getKey().equals(fieldName)) {
					return jsonObject;
				} else {
					searchJsonElement(fieldName, entry.getValue());
				}
			}
		} else if (base.isJsonArray()) {
			for (JsonElement element : base.getAsJsonArray()) {
				JsonObject ob = searchJsonElement(fieldName, element);
				if (ob != null) {
					return ob;
				}
			}
		}
		return null;
	}
}
