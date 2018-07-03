package org.vadere.simulator.projects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesCar;
import org.vadere.state.scenario.Topography;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.data.FindByClass;
import org.vadere.util.reflection.VadereClassNotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Contains the data for a Vadere object that can be serialized.
 * 
 *
 */
public class ScenarioStore {

	private static Logger logger = LogManager.getLogger(ScenarioStore.class);
	public String name;
	public String description;
	public String mainModel;
	public List<Attributes> attributesList;
	public AttributesSimulation attributesSimulation;
	private Topography topography;

	public ScenarioStore(final String name, final String description, final String mainModel, final List<Attributes> attributesModel,
			final AttributesSimulation attributesSimulation, final Topography topography) {
		this.name = name;
		this.description = description;
		this.mainModel = mainModel;
		this.attributesList = attributesModel;
		this.attributesSimulation = attributesSimulation;
		this.topography = topography;
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
		return DigestUtils.sha1Hex(StateJsonConverter.serializeObject(this));
	}
	
	public void sealAllAttributes() {
		attributesList.forEach(a -> a.seal());
		attributesSimulation.seal();
		topography.sealAllAttributes();
	}

	public <T extends Attributes> T getAttributes(@NotNull final Class<T> clazz) {
        return FindByClass.findSingleObjectOfClass(attributesList, clazz);
    }
}
