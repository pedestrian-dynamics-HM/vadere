package org.vadere.simulator.projects;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.attributes.scenario.AttributesCar;
import org.vadere.state.events.json.EventInfoStore;
import org.vadere.state.scenario.Topography;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.data.FindByClass;
import org.vadere.util.logging.Logger;
import org.vadere.util.reflection.VadereClassNotFoundException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Contains the data for a Vadere object that can be serialized.
 * 
 *
 */
public class ScenarioStore {

	private static Logger logger = Logger.getLogger(ScenarioStore.class);
	private String name;
	private String description;
	private String mainModel;
	private List<Attributes> attributesList;
	private AttributesSimulation attributesSimulation;
	private Topography topography;
	private EventInfoStore eventInfoStore;

	public ScenarioStore(final String name, final String description, final String mainModel, final List<Attributes> attributesModel,
			final AttributesSimulation attributesSimulation, final Topography topography) {
	    // Provide empty "EventInfoStore".
		this(name, description, mainModel, attributesModel, attributesSimulation, topography, new EventInfoStore());
	}

	public ScenarioStore(final String name, final String description, final String mainModel, final List<Attributes> attributesModel,
						 final AttributesSimulation attributesSimulation, final Topography topography, final EventInfoStore eventInfoStore) {
		this.name = name;
		this.description = description;
		this.mainModel = mainModel;
		this.attributesList = attributesModel;
		this.attributesSimulation = attributesSimulation;
		this.topography = topography;
		this.eventInfoStore = eventInfoStore;
	}

	public synchronized Topography getTopography() {
		return topography;
	}

	public synchronized void setTopography(final Topography topography) {
		//logger.info("setTopography:" + topography + ", thread:" + Thread.currentThread());
		this.topography = topography;
	}

	public ScenarioStore(final String name) {
		this(name, "", null, new ArrayList<>(), new AttributesSimulation(), new Topography());
	}

	public AttributesCar getAttributesCar() {
		return topography.getAttributesCar();
	}

	@Override
	public ScenarioStore clone() {
		try {
			return JsonConverter.cloneScenarioStore(this);
		} catch (IOException | VadereClassNotFoundException e) {
			throw new RuntimeException(e);
			// Do not return null or Optional, that does not make sense!
		}
	}
	
	public String hashOfJsonRepresentation() throws JsonProcessingException {
		return StateJsonConverter.getScenarioStoreHash(this);
	}
	
	public void sealAllAttributes() {
		attributesList.forEach(a -> a.seal());
		attributesSimulation.seal();
		topography.sealAllAttributes();
	}

	public <T extends Attributes> T getAttributes(@NotNull final Class<T> clazz) {
        return FindByClass.findSingleObjectOfClass(attributesList, clazz);
    }

	public void setAttributesList(final List<Attributes> attributesList) {
		this.attributesList = attributesList;
	}

	public void setAttributesSimulation(final AttributesSimulation attributesSimulation) {
		this.attributesSimulation = attributesSimulation;
	}

	public void removeAttributesIf(@NotNull final Predicate<Attributes> predicate) {
		attributesList.removeIf(predicate);
	}

	public void addAttributes(@NotNull final Attributes attributes) {
		attributesList.add(attributes);
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public void setMainModel(final String mainModel) {
		this.mainModel = mainModel;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setEventInfoStore(EventInfoStore eventInfoStore) { this.eventInfoStore = eventInfoStore; }

	public AttributesSimulation getAttributesSimulation() {
		return attributesSimulation;
	}

	public List<Attributes> getAttributesList() {
		return attributesList;
	}

	public String getDescription() {
		return description;
	}

	public String getMainModel() {
		return mainModel;
	}

	public String getName() {
		return name;
	}

	public EventInfoStore getEventInfoStore() { return eventInfoStore; }
}
