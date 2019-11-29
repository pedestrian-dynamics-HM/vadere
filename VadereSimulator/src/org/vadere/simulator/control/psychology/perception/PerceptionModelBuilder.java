package org.vadere.simulator.control.psychology.perception;

import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.SubModelBuilder;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.util.reflection.DynamicClassInstantiator;

import java.util.Random;

/**
 * This class encapsulates the creation of a concrete {@link IPerceptionModel}
 * which is defined by the user in the JSON scenario description.
 *
 * The user provides the simple class name in the JSON scenario file.
 * I.e., no fully qualified classname.
 */
public class PerceptionModelBuilder {

	public static final String JAVA_PACKAGE_SEPARATOR = ".";

	public static IPerceptionModel instantiateModel(ScenarioStore scenarioStore) {
		String simpleClassName = scenarioStore.getAttributesPsychology().getPsychologyLayer().getPerception();
		String classSearchPath = IPerceptionModel.class.getPackageName();
		String fullyQualifiedClassName = classSearchPath + JAVA_PACKAGE_SEPARATOR + simpleClassName;

		DynamicClassInstantiator<IPerceptionModel> instantiator = new DynamicClassInstantiator<>();
		IPerceptionModel perceptionModel = instantiator.createObject(fullyQualifiedClassName);

		perceptionModel.initialize(scenarioStore.getTopography());

		return perceptionModel;
	}

}
