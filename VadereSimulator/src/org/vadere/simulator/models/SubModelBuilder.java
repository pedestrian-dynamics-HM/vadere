package org.vadere.simulator.models;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.vadere.simulator.control.ActiveCallback;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.reflection.DynamicClassInstantiator;

/**
 * Helper class to build submodels of a main model and add them to a
 * ActiveCallback list.
 */
public class SubModelBuilder {
	
	private final List<Attributes> modelAttributesList;
	private final Topography topography;
	private final AttributesAgent attributesPedestrian;
	private final Random random;

	private final List<Model> subModels = new LinkedList<>();

	public SubModelBuilder(List<Attributes> modelAttributesList, Topography topography,
			AttributesAgent attributesPedestrian, Random random) {
		this.modelAttributesList = modelAttributesList;
		this.topography = topography;
		this.attributesPedestrian = attributesPedestrian;
		this.random = random;
	}

	public void buildSubModels(List<String> subModelClassNames) {
		for (String submodelName : subModelClassNames) {
			final DynamicClassInstantiator<Model> modelInstantiator = new DynamicClassInstantiator<>();
			final Model submodel = modelInstantiator.createObject(submodelName);
			submodel.initialize(modelAttributesList, topography, attributesPedestrian, random);
			subModels.add(submodel);
		}
	}

	/**
	 * Add submodels to a ActiveCallback list.
	 *
	 * Maybe in future <code>getSubModels()</code> instead? Currently, this is
	 * the best way to avoid redundancy of adding them to the list.
	 */
	public void addSubModelsToActiveCallbacks(List<ActiveCallback> activeCallbacks) {
		for (Model model : subModels) {
			if (model instanceof ActiveCallback) {
				activeCallbacks.add((ActiveCallback) model);
			}
		}
	}

}
