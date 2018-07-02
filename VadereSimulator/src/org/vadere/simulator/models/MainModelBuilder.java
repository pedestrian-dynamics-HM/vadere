package org.vadere.simulator.models;

import java.util.Random;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
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

	private Logger logger = LogManager.getLogger(MainModelBuilder.class);
	private ScenarioStore scenarioStore;
	private MainModel model;
	private Random random;

	public MainModelBuilder(ScenarioStore scenarioStore) {
		logger.info("0:"+scenarioStore.topography);
		this.scenarioStore = scenarioStore;
		logger.info("1:"+scenarioStore.topography);
	}

	public void createModelAndRandom()
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		logger.info("2:"+scenarioStore.topography);
		final AttributesSimulation attributesSimulation = scenarioStore.attributesSimulation;
		if (attributesSimulation.isUseRandomSeed()) {
			random = new Random(attributesSimulation.getRandomSeed());
		} else {
			random = new Random();
		}

		model = instantiateMainModel(random);
		logger.info("3:"+scenarioStore.topography);

	}

	public MainModel getModel() {
		return model;
	}

	public Random getRandom() {
		return random;
	}

	private MainModel instantiateMainModel(Random random) {
		logger.info("4:"+scenarioStore.topography);
		String mainModelName = scenarioStore.mainModel;
		logger.info("5:"+scenarioStore.topography);
		DynamicClassInstantiator<MainModel> instantiator = new DynamicClassInstantiator<>();
		logger.info("6:"+scenarioStore.topography);
		MainModel mainModel = instantiator.createObject(mainModelName);
		logger.info("7:"+scenarioStore.topography);
		mainModel.initialize(scenarioStore.attributesList, scenarioStore.topography,
				scenarioStore.topography.getAttributesPedestrian(), random);
		logger.info("8:"+scenarioStore.topography);
		return mainModel;
	}

}
