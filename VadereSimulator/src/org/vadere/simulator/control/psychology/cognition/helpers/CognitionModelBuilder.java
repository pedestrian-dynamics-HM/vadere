package org.vadere.simulator.control.psychology.cognition.helpers;

import org.vadere.simulator.control.psychology.cognition.models.ICognitionModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.psychology.cognition.AttributesCognitionModel;
import org.vadere.util.reflection.DynamicClassInstantiator;

import java.util.List;
import java.util.Random;

/**
 * This class encapsulates the creation of a concrete {@link ICognitionModel}
 * which is defined by the user in the JSON scenario description.
 *
 * The user provides the simple class name in the JSON scenario file.
 * I.e., no fully qualified classname.
 */
public class CognitionModelBuilder {

	public static final String JAVA_PACKAGE_SEPARATOR = ".";

	public static ICognitionModel instantiateModel(final ScenarioStore scenarioStore) {
		String simpleClassName = scenarioStore.getAttributesPsychology().getPsychologyLayer().getCognition();
		String classSearchPath = ICognitionModel.class.getPackageName();
		String fullyQualifiedClassName = classSearchPath + JAVA_PACKAGE_SEPARATOR + simpleClassName;

		DynamicClassInstantiator<ICognitionModel> instantiator = new DynamicClassInstantiator<>();
		ICognitionModel cognitionModel = instantiator.createObject(fullyQualifiedClassName);

		List<Attributes> attributes = scenarioStore.getAttributesPsychology().getPsychologyLayer().getAttributesModel();

		Random random = new Random(scenarioStore.getAttributesSimulation().getSimulationSeed());
		cognitionModel.initialize(scenarioStore.getTopography(), random);

		List<Attributes> attributesList = scenarioStore.getAttributesPsychology().getPsychologyLayer().getAttributesModel();

		AttributesCognitionModel attributesCognitionModel = Model.findAttributes(attributesList, cognitionModel.getAttributes().getClass());
		cognitionModel.setAttributes(attributesCognitionModel);

		return cognitionModel;
	}

}
