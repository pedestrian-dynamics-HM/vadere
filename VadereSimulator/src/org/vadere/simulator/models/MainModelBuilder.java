package org.vadere.simulator.models;

import java.util.Random;

import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.util.reflection.DynamicClassInstantiator;

/**
 * This class encapsulates the creation of MainModel.
 * 
 * For creation of submodels, see {@link SubModelBuilder}! The SubModelBuilder
 * should be used in the {@link MainModel#initialize} method.
 */
public class MainModelBuilder {

	private ScenarioStore scenarioStore;
	private MainModel model;
	private Random random;

	public MainModelBuilder(ScenarioStore scenarioStore) {
		this.scenarioStore = scenarioStore;
	}

	public void createModelAndRandom()
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		final AttributesSimulation attributesSimulation = scenarioStore.attributesSimulation;
		if (attributesSimulation.isUseRandomSeed()) {
			random = new Random(attributesSimulation.getRandomSeed());
		} else {
			random = new Random();
		}

		model = instantiateMainModel(random);

	}

	public MainModel getModel() {
		return model;
	}

	public Random getRandom() {
		return random;
	}

	private MainModel instantiateMainModel(Random random) {
		String mainModelName = scenarioStore.mainModel;
		DynamicClassInstantiator<MainModel> instantiator = new DynamicClassInstantiator<>();
		MainModel mainModel = instantiator.createObject(mainModelName);
		mainModel.initialize(scenarioStore.attributesList, scenarioStore.topography,
				scenarioStore.topography.getAttributesPedestrian(), random);
		return mainModel;
	}

}
