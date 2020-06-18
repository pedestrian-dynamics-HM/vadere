package org.vadere.simulator.control.psychology.perception.helpers;

import org.vadere.simulator.control.psychology.perception.models.IPerceptionModel;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.util.reflection.DynamicClassInstantiator;

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
