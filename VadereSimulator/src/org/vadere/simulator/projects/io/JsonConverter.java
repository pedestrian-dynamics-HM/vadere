package org.vadere.simulator.projects.io;

import java.io.IOException;
import java.util.List;

import org.vadere.state.attributes.*;
import org.vadere.util.version.Version;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.simulator.projects.dataprocessing.DataProcessingJsonManager;
import org.vadere.state.psychology.perception.json.StimulusInfoStore;
import org.vadere.state.scenario.Topography;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.reflection.DynamicClassInstantiator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonConverter {
	
	public static Scenario deserializeScenarioRunManager(String json) throws IOException, IllegalArgumentException {
		return deserializeScenarioRunManagerFromNode(StateJsonConverter.readTree(json));
	}

	public static ModelDefinition deserializeModelDefinition(String json) throws IOException {
		JsonNode node = StateJsonConverter.readTree(json);
		StateJsonConverter.checkForTextOutOfNode(json);
		if (!node.has(StateJsonConverter.MAIN_MODEL_KEY))
			throw new IOException("No " + StateJsonConverter.MAIN_MODEL_KEY + "-entry was found.");
		String mainModelString = null;
		JsonNode mainModel = node.get(StateJsonConverter.MAIN_MODEL_KEY);
		if (!mainModel.isNull()) { // avoid test-instantiating when mainModel isn't set, otherwise user has invalid json when creating a new scenario
			DynamicClassInstantiator<MainModel> instantiator = new DynamicClassInstantiator<>();
			mainModelString = mainModel.asText();
			// instantiate to get an topographyError if the string can't be mapped onto a model
			@SuppressWarnings("unused")
			MainModel dummyToProvokeClassCast = instantiator.createObject(mainModelString);
		}
		return new ModelDefinition(mainModelString, StateJsonConverter.deserializeAttributesListFromNode(node.get("attributesModel")));
	}

	public static Scenario deserializeScenarioRunManagerFromNode(JsonNode node) throws IOException, IllegalArgumentException {
		JsonNode rootNode = node;
		JsonNode scenarioNode = rootNode.get(StateJsonConverter.SCENARIO_KEY);

		String scenarioName = rootNode.get("name").asText();
		String scenarioDescription = rootNode.get("description").asText();

		AttributesSimulation attributesSimulation = StateJsonConverter.deserializeAttributesSimulationFromNode(scenarioNode.get(AttributesSimulation.JSON_KEY));
		AttributesPsychology attributesPsychology = StateJsonConverter.deserializeAttributesPsychologyFromNode(scenarioNode.get(AttributesPsychology.JSON_KEY));


		JsonNode attributesModelNode = scenarioNode.get("attributesModel");
		String mainModel = (!scenarioNode.has(StateJsonConverter.MAIN_MODEL_KEY) ||
				scenarioNode.get(StateJsonConverter.MAIN_MODEL_KEY).isNull()) ? null : scenarioNode.get(StateJsonConverter.MAIN_MODEL_KEY).asText();
		List<Attributes> attributesModel = StateJsonConverter.deserializeAttributesListFromNode(attributesModelNode);
		Topography topography = StateJsonConverter.deserializeTopographyFromNode(scenarioNode.get("topography"));
		StimulusInfoStore stimulusInfoStore = StateJsonConverter.deserializeStimuliFromArrayNode(scenarioNode);

		ScenarioStore scenarioStore = new ScenarioStore(scenarioName, scenarioDescription,
				mainModel, attributesModel,
				attributesSimulation, attributesPsychology,
				topography, stimulusInfoStore);
		Scenario scenarioRunManager = new Scenario(scenarioStore);

		scenarioRunManager.setDataProcessingJsonManager(DataProcessingJsonManager.deserializeFromNode(rootNode.get(DataProcessingJsonManager.DATAPROCCESSING_KEY)));
		scenarioRunManager.saveChanges();

		return scenarioRunManager;
	}



	// used in hasUnsavedChanges, TODO [priority=high] [task=bugfix] check if commitHashIncluded can always be false
	public static String serializeScenarioRunManager(Scenario scenarioRunManager) {
		try {
			return serializeScenarioRunManager(scenarioRunManager, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String serializeScenarioRunManager(Scenario scenarioRunManager, boolean commitHashIncluded)
			throws IOException {
		return StateJsonConverter.writeValueAsString(serializeScenarioRunManagerToNode(scenarioRunManager, commitHashIncluded));
	}

	public static JsonNode serializeScenarioRunManagerToNode(Scenario scenarioRunManager, boolean commitHashIncluded) {
		ScenarioStore scenarioStore = scenarioRunManager.getScenarioStore();
		ObjectNode rootNode = StateJsonConverter.createObjectNode();
		serializeMeta(rootNode, commitHashIncluded, scenarioStore);
		rootNode.set(DataProcessingJsonManager.DATAPROCCESSING_KEY, scenarioRunManager.getDataProcessingJsonManager().serializeToNode());
		rootNode.set(StateJsonConverter.SCENARIO_KEY, serializeVadereNode(scenarioStore));
		return rootNode;
	}

	private static void serializeMeta(ObjectNode node, boolean commitHashIncluded, ScenarioStore scenarioStore) {
		node.put("name", scenarioStore.getName());
		node.put("description", scenarioStore.getDescription());
		node.put("release", Version.releaseNumber());
		if (commitHashIncluded)
			node.put("commithash", Version.getVersionControlCommitHash());
	}

	private static ObjectNode serializeVadereNode(ScenarioStore scenarioStore) {
		ObjectNode vadereNode = StateJsonConverter.createObjectNode();

		vadereNode.put(StateJsonConverter.MAIN_MODEL_KEY, scenarioStore.getMainModel());

		ObjectNode attributesModelNode = StateJsonConverter.serializeAttributesModelToNode(scenarioStore.getAttributesList());
		vadereNode.set("attributesModel", attributesModelNode);

		vadereNode.set(AttributesSimulation.JSON_KEY, StateJsonConverter.convertValue(scenarioStore.getAttributesSimulation(), JsonNode.class));

		ObjectNode psychologyNode = StateJsonConverter.serializeAttributesPsychologyToNode(scenarioStore.getAttributesPsychology());
		vadereNode.set(AttributesPsychology.JSON_KEY, psychologyNode);

		ObjectNode topographyNode = StateJsonConverter.serializeTopographyToNode(scenarioStore.getTopography());
		vadereNode.set("topography", topographyNode);

		// We get a complete tree here and not only a node. Therefore, use "setAll()" instead of "set()".
		ObjectNode eventNode = StateJsonConverter.serializeStimuliToNode(scenarioStore.getStimulusInfoStore());
		vadereNode.setAll(eventNode);

		return vadereNode;
	}

	public static Scenario cloneScenarioRunManager(Scenario original) throws IOException {
		JsonNode clone = serializeScenarioRunManagerToNode(original, false);
		return deserializeScenarioRunManagerFromNode(clone);
	}

	public static ScenarioStore cloneScenarioStore(ScenarioStore scenarioStore) throws IOException {
		JsonNode attributesSimulationNode = StateJsonConverter.convertValue(scenarioStore.getAttributesSimulation(), JsonNode.class);

		ObjectNode attributesPsychologyNode = StateJsonConverter.serializeAttributesPsychologyToNode(scenarioStore.getAttributesPsychology());
		ObjectNode attributesModelNode = StateJsonConverter.serializeAttributesModelToNode(scenarioStore.getAttributesList());
		ObjectNode topographyNode = StateJsonConverter.serializeTopographyToNode(scenarioStore.getTopography());
		ObjectNode stimulusNode = StateJsonConverter.serializeStimuliToNode(scenarioStore.getStimulusInfoStore());
		JsonNode stimulusInfosArrayNode = stimulusNode.get("stimulusInfos");


		if (stimulusInfosArrayNode == null) {
			throw new IOException("Cannot clone scenario: No stimuli found!");
		}

		return new ScenarioStore(scenarioStore.getName(), scenarioStore.getDescription(), scenarioStore.getMainModel(),
				StateJsonConverter.deserializeAttributesListFromNode(attributesModelNode),
				StateJsonConverter.deserializeAttributesSimulationFromNode(attributesSimulationNode),
				StateJsonConverter.deserializeAttributesPsychologyFromNode(attributesPsychologyNode),
				StateJsonConverter.deserializeTopographyFromNode(topographyNode),
				StateJsonConverter.deserializeStimuliFromArrayNode(stimulusNode)
				);
	}
}
