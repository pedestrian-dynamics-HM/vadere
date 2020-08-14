package org.vadere.simulator.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.simulator.projects.Domain;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.util.reflection.DynamicClassInstantiator;

import java.util.Random;

/**
 * This class encapsulates the creation of MainModel.
 * 
 * For creation of submodels, see {@link SubModelBuilder}! The SubModelBuilder
 * should be used in the {@link MainModel#initialize} method.
 */
public class MainModelBuilder {

	private ScenarioStore scenarioStore;
	private MainModel model;
	private Domain domain;
	private Random random;

	public MainModelBuilder(@NotNull final ScenarioStore scenarioStore, @Nullable final AMesh floorFieldMesh, @Nullable final AMesh backgroundMesh) {
		this.scenarioStore = scenarioStore;
		this.domain = new Domain(floorFieldMesh, backgroundMesh, scenarioStore.getTopography());
	}

	public void createModelAndRandom()
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		final AttributesSimulation attributesSimulation = scenarioStore.getAttributesSimulation();
		if (attributesSimulation.isUseFixedSeed()) {
			long seed = attributesSimulation.getFixedSeed();
			attributesSimulation.setSimulationSeed(seed);
			random = new Random(seed);
		} else {
			long seed = new Random().nextLong();
			attributesSimulation.setSimulationSeed(seed);
			random = new Random(seed);
		}

		model = instantiateMainModel(random);

	}

	public MainModel getModel() {
		return model;
	}

	public Random getRandom() {
		return random;
	}

	public Domain getDomain() {
		return domain;
	}

	private MainModel instantiateMainModel(Random random) {
		String mainModelName = scenarioStore.getMainModel();
		DynamicClassInstantiator<MainModel> instantiator = new DynamicClassInstantiator<>();
		MainModel mainModel = instantiator.createObject(mainModelName);
		mainModel.initialize(scenarioStore.getAttributesList(), getDomain(),
				scenarioStore.getTopography().getAttributesPedestrian(), random);
		return mainModel;
	}

}
