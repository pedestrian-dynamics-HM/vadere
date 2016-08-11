package org.vadere.simulator.projects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.attributes.scenario.AttributesCar;
import org.vadere.state.scenario.Topography;
import org.vadere.util.reflection.VadereClassNotFoundException;

/**
 * Contains the data for a Vadere object that can be serialized.
 * 
 *
 */
public class ScenarioStore {

	public String name;
	public String description;
	public String mainModel;
	public List<Attributes> attributesList;
	public AttributesSimulation attributesSimulation;
	public Topography topography;

	public ScenarioStore(final String name, final String description, final String mainModel, final List<Attributes> attributesModel,
			final AttributesSimulation attributesSimulation, final Topography topography) {
		this.name = name;
		this.description = description;
		this.mainModel = mainModel;
		this.attributesList = attributesModel;
		this.attributesSimulation = attributesSimulation;
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

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ScenarioStore that = (ScenarioStore) o;

		if (attributesList != null ? !attributesList.equals(that.attributesList) : that.attributesList != null)
			return false;
		if (attributesSimulation != null ? !attributesSimulation.equals(that.attributesSimulation)
				: that.attributesSimulation != null)
			return false;
		if (topography != null ? !topography.equals(that.topography) : that.topography != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = attributesList != null ? attributesList.hashCode() : 0;
		result = 31 * result + (attributesSimulation != null ? attributesSimulation.hashCode() : 0);
		result = 31 * result + (topography != null ? topography.hashCode() : 0);
		return result;
	}
}
