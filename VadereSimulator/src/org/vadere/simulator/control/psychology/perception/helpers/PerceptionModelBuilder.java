package org.vadere.simulator.control.psychology.perception.helpers;

import org.vadere.simulator.control.psychology.perception.models.IPerceptionModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.psychology.perception.AttributesPerceptionModel;
import org.vadere.state.scenario.Topography;
import org.vadere.util.reflection.DynamicClassInstantiator;

import java.util.List;

/**
 * This class encapsulates the creation of a concrete {@link IPerceptionModel}
 * which is defined by the user in the JSON scenario description.
 *
 * The user provides the simple class name in the JSON scenario file.
 * I.e., no fully qualified classname.
 */
public class PerceptionModelBuilder {

	public static final String JAVA_PACKAGE_SEPARATOR = ".";

	public static IPerceptionModel instantiateModel(final ScenarioStore scenarioStore) {
		String simpleClassName = scenarioStore.getAttributesPsychology().getPsychologyLayer().getPerception();
		String classSearchPath = IPerceptionModel.class.getPackageName();
		String fullyQualifiedClassName = classSearchPath + JAVA_PACKAGE_SEPARATOR + simpleClassName;

		DynamicClassInstantiator<IPerceptionModel> instantiator = new DynamicClassInstantiator<>();
		IPerceptionModel perceptionModel = instantiator.createObject(fullyQualifiedClassName);

		Topography topography = scenarioStore.getTopography();
		double simTimeStepLength = scenarioStore.getAttributesSimulation().getSimTimeStepLength();
		List<Attributes> attributesList = scenarioStore.getAttributesPsychology().getPsychologyLayer().getAttributesModel();

		perceptionModel.initialize(topography, simTimeStepLength);

		AttributesPerceptionModel attributes = Model.findAttributes(attributesList, perceptionModel.getAttributes().getClass());
		perceptionModel.setAttributes(attributes);



		return perceptionModel;
	}

}
