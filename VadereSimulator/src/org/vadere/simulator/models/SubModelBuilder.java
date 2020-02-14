package org.vadere.simulator.models;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.reflection.DynamicClassInstantiator;

/**
 * Helper class to build submodels of a main model and add them to a
 * list of models.
 */
public class SubModelBuilder {
	
	private final List<Attributes> modelAttributesList;
	private final Domain domain;
	private final AttributesAgent attributesPedestrian;
	private final Random random;

	private final List<Model> subModels = new LinkedList<>();

	public SubModelBuilder(List<Attributes> modelAttributesList, Domain domain,
			AttributesAgent attributesPedestrian, Random random) {
		this.modelAttributesList = modelAttributesList;
		this.domain = domain;
		this.attributesPedestrian = attributesPedestrian;
		this.random = random;
	}

	public void buildSubModels(List<String> subModelClassNames) {
		for (String submodelName : subModelClassNames) {
			final DynamicClassInstantiator<Model> modelInstantiator = new DynamicClassInstantiator<>();
			final Model submodel = modelInstantiator.createObject(submodelName);
			submodel.initialize(modelAttributesList, domain, attributesPedestrian, random);
			subModels.add(submodel);
		}
	}

	/** Add the builded submodels to the client's list of models. */
	public void addBuildedSubModelsToList(List<Model> modelList) {
		// Maybe in future getSubModels() instead? Currently, this is
		// the best way to avoid the redundancy of adding them to the list.
		modelList.addAll(subModels);
	}

}
