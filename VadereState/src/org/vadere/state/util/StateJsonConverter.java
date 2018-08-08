package org.vadere.state.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.attributes.ModelDefinition;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesCar;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.AttributesStairs;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.attributes.scenario.AttributesTeleporter;
import org.vadere.state.attributes.scenario.AttributesTopography;
import org.vadere.state.events.json.EventInfo;
import org.vadere.state.events.json.EventInfoStore;
import org.vadere.state.events.types.Event;
import org.vadere.state.scenario.Car;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Stairs;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Teleporter;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.reflection.DynamicClassInstantiator;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class StateJsonConverter {

	public static final String SCENARIO_KEY = "scenario";

	public static final String MAIN_MODEL_KEY = "mainModel";

	private static final TypeReference<Map<String, Object>> mapTypeReference =
			new TypeReference<Map<String, Object>>() {};

	private static Logger logger = LogManager.getLogger(StateJsonConverter.class);

	/** Connection to jackson library. */
	private static ObjectMapper mapper = new JacksonObjectMapper();

	/** Connection to jackson library. */
	private static ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

	public static ObjectMapper getMapper() {
		return mapper;
	}
	
	// TODO handle exception
	public static <T> T deserializeObjectFromJson(String json, Class<T> objectClass) {
		
		try {
			final JsonNode node = mapper.readTree(json);
			checkForTextOutOfNode(json);
			return mapper.treeToValue(node, objectClass);
		} catch (TextOutOfNodeException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	// - - - - DESERIALIZING - - - -

	public static JsonNode deserializeToNode(String dev) throws IOException {
		return mapper.readTree(dev);
	}

	public static Object deserializeToMapOfObjects(String dev) throws IOException {
		return mapper.readValue(dev, mapTypeReference);
	}

	public static Object convertJsonNodeToObject(JsonNode node) {
		return mapper.convertValue(node, Map.class);
	}

	public static JsonNode deserializeToNode(Object map){
		return mapper.valueToTree(map);
	}

	private static class TopographyStore {
		AttributesTopography attributes = new AttributesTopography();
		AttributesAgent attributesPedestrian = new AttributesAgent();
		AttributesCar attributesCar = new AttributesCar();
		Collection<AttributesObstacle> obstacles = new LinkedList<>();
		Collection<AttributesStairs> stairs = new LinkedList<>();
		Collection<AttributesTarget> targets = new LinkedList<>();
		Collection<AttributesSource> sources = new LinkedList<>();
		Collection<? extends DynamicElement> dynamicElements = new LinkedList<>();
		Collection<Event> events = new LinkedList<>();
		AttributesTeleporter teleporter = null;
	}

	public static AttributesSimulation deserializeAttributesSimulation(String json) {
		return deserializeObjectFromJson(json, AttributesSimulation.class);
	}

	public static AttributesSimulation deserializeAttributesSimulationFromNode(JsonNode node)
			throws JsonProcessingException {
		return mapper.treeToValue(node, AttributesSimulation.class);
	}

	

	public static List<Attributes> deserializeAttributesListFromNode(JsonNode node) throws JsonProcessingException {
		DynamicClassInstantiator<Attributes> instantiator = new DynamicClassInstantiator<>();
		List<Attributes> attributesList = new LinkedList<>();
		Iterator<Map.Entry<String, JsonNode>> it = node.fields();
		while (it.hasNext()) {
			Map.Entry<String, JsonNode> entry = it.next();
			Attributes attributes = mapper.treeToValue(entry.getValue(), instantiator.getClassFromName(entry.getKey()));
			attributesList.add(attributes);
		}
		return attributesList;
	}

	public static Topography deserializeTopography(String json) throws IOException, TextOutOfNodeException {
		checkForTextOutOfNode(json);
		return deserializeTopographyFromNode(mapper.readTree(json));
	}

	public static Topography deserializeTopographyFromNode(JsonNode node) throws IllegalArgumentException {
		TopographyStore store = mapper.convertValue(node, TopographyStore.class);
		Topography topography = new Topography(store.attributes, store.attributesPedestrian, store.attributesCar);
		store.obstacles.forEach(obstacle -> topography.addObstacle(new Obstacle(obstacle)));
		store.stairs.forEach(stairs -> topography.addStairs(new Stairs(stairs)));
		store.targets.forEach(target -> topography.addTarget(new Target(target)));
		store.sources.forEach(source -> topography.addSource(new Source(source)));
		store.dynamicElements.forEach(topography::addInitialElement);
		if (store.teleporter != null)
			topography.setTeleporter(new Teleporter(store.teleporter));
		return topography;
	}

	public static void checkForTextOutOfNode(String json) throws TextOutOfNodeException, IOException { // via stackoverflow.com/a/26026359
		JsonParser jp = mapper.getFactory().createParser(json);
		mapper.readValue(jp, JsonNode.class);
		try {
			if (jp.nextToken() != null)
				throw new TextOutOfNodeException();
		} catch (IOException e) { // can be thrown in nextToken()
			throw new TextOutOfNodeException();
		} finally {
			jp.close();
		}
	}

	/**
	 * Pass a node representing an array of @see EventInfo objects.
	 *
	 * Usually, this array is extracted by reading in a scenario file as @see JsonNode
	 * an you call "get("eventInfos") on this @see JsonNode.
	 */
	public static EventInfoStore deserializeEventsFromNode(JsonNode node) throws IllegalArgumentException {
		EventInfoStore eventInfoStore = new EventInfoStore();

		if (node != null) {
			List<EventInfo> eventInfoList = new ArrayList<>();
			node.forEach(eventInfoNode -> eventInfoList.add(mapper.convertValue(eventInfoNode, EventInfo.class)));
			eventInfoStore.setEventInfos(eventInfoList);
		}

		return eventInfoStore;
	}

	public static EventInfoStore deserializeEvents(String json) throws IOException {
		return mapper.readValue(json, EventInfoStore.class);
	}

	public static Pedestrian deserializePedestrian(String json) throws IOException {
		return mapper.readValue(json, Pedestrian.class);
	}

	public static Car deserializeCar(String json) throws IOException {
		return mapper.readValue(json, Car.class);
	}

	public static Attributes deserializeScenarioElementType(String json, ScenarioElementType type) throws IOException {
		// TODO [priority=low] [task=refactoring] find a better way!
		switch (type) {
			case OBSTACLE:
				return mapper.readValue(json, AttributesObstacle.class);
			case PEDESTRIAN:
				return mapper.readValue(json, AttributesAgent.class);
			case SOURCE:
				return mapper.readValue(json, AttributesSource.class);
			case TARGET:
				return mapper.readValue(json, AttributesTarget.class);
			case STAIRS:
				return mapper.readValue(json, AttributesStairs.class);
			case TELEPORTER:
				return mapper.readValue(json, AttributesTeleporter.class);
			case CAR:
				return mapper.readValue(json, AttributesCar.class);
			default:
				return null;
		}
	}

	// - - - - SERIALIZING - - - -

	// could also serialize each ModelType individually and add the strings with comma and brackets
	// in between - but building a proper mini-Json-ObjectNode seems a more stable and elegant solution
	public static String serializeModelPreset(ModelDefinition modelDefinition)
			throws JsonProcessingException { // may also throw one of the instantiator's exceptions

		ObjectNode node = mapper.createObjectNode();
		node.put(MAIN_MODEL_KEY, modelDefinition.getMainModel());
		node.set("attributesModel", serializeAttributesModelToNode(modelDefinition.getAttributesList()));
		return writer.writeValueAsString(node);
	}

	public static ObjectNode serializeAttributesModelToNode(final List<Attributes> attributesList) {
		List<Pair<String, Attributes>> attributePairList = attributesListToNameObjectPairList(attributesList);

		ObjectNode attributesModelNode = mapper.createObjectNode();
		attributePairList.forEach(pair -> attributesModelNode.set(pair.getLeft(),
				mapper.convertValue(pair.getRight(), JsonNode.class)));

		return attributesModelNode;
	}

	private static List<Pair<String, Attributes>> attributesListToNameObjectPairList(List<Attributes> attributesList) {
		List<Pair<String, Attributes>> list = new ArrayList<>(attributesList.size());
		for (Attributes a : attributesList)
			list.add(Pair.of(a.getClass().getName(), a));
		return list;
	}

	public static ObjectNode serializeTopographyToNode(Topography topography) {
		ObjectNode topographyNode = mapper.createObjectNode();

		JsonNode attributesNode = mapper.convertValue(topography.getAttributes(), JsonNode.class);
		// manually remove that field to match the old gson-format, seems easier than avoiding it selectively
		// TODO what does this mean?
		((ObjectNode) attributesNode.get("bounds")).remove("type");
		topographyNode.set("attributes", attributesNode);

		ArrayNode obstacleNodes = mapper.createArrayNode();
		topography.getObstacles()
				.forEach(obstacle -> obstacleNodes.add(mapper.convertValue(obstacle.getAttributes(), JsonNode.class)));
		topographyNode.set("obstacles", obstacleNodes);

		ArrayNode stairNodes = mapper.createArrayNode();
		topography.getStairs()
				.forEach(stair -> stairNodes.add(mapper.convertValue(stair.getAttributes(), JsonNode.class)));
		topographyNode.set("stairs", stairNodes);

		ArrayNode targetNodes = mapper.createArrayNode();
		topography.getTargets()
				.forEach(target -> targetNodes.add(mapper.convertValue(target.getAttributes(), JsonNode.class)));
		topographyNode.set("targets", targetNodes);

		ArrayNode sourceNodes = mapper.createArrayNode();
		topography.getSources()
				.forEach(source -> sourceNodes.add(mapper.convertValue(source.getAttributes(), JsonNode.class)));
		topographyNode.set("sources", sourceNodes);

		ArrayNode dynamicElementNodes = mapper.createArrayNode();
		topography.getPedestrianDynamicElements().getInitialElements()
				.forEach(ped -> dynamicElementNodes.add(mapper.convertValue(ped, JsonNode.class))); // TODO [priority=medium] [task=check] initial elements is the right list, isn't it?
		topography.getCarDynamicElements().getInitialElements()
				.forEach(car -> dynamicElementNodes.add(mapper.convertValue(car, JsonNode.class))); // TODO [priority=medium] [task=test] verify that this works
		topographyNode.set("dynamicElements", dynamicElementNodes);

		JsonNode attributesPedestrianNode = mapper.convertValue(topography.getAttributesPedestrian(), JsonNode.class);
		topographyNode.set("attributesPedestrian", attributesPedestrianNode);
		if (attributesPedestrianNode != null)
			((ObjectNode) attributesPedestrianNode).remove("id");

		JsonNode attributesCarNode = mapper.convertValue(topography.getAttributesCar(), JsonNode.class);
		topographyNode.set("attributesCar", attributesCarNode);

		return topographyNode;
	}

	public static String serializeAttributesSimulation(AttributesSimulation attributesSimulation)
			throws JsonProcessingException {
		return writer.writeValueAsString(mapper.convertValue(attributesSimulation, JsonNode.class));
	}

	public static String serializeTopography(Topography topography) throws JsonProcessingException {
		return writer.writeValueAsString(serializeTopographyToNode(topography));
	}

	public static String serializeMainModelAttributesModelBundle(List<Attributes> attributesList, String mainModel)
			throws JsonProcessingException {
		ObjectNode node = mapper.createObjectNode();
		node.put(MAIN_MODEL_KEY, mainModel);
		node.set("attributesModel", serializeAttributesModelToNode(attributesList));
		return writer.writeValueAsString(node);
	}

	public static String serializeEvents(EventInfoStore eventInfoStore)
			throws JsonProcessingException {
		return writer.writeValueAsString(mapper.convertValue(eventInfoStore, JsonNode.class));
	}

	public static ObjectNode serializeEventsToNode(EventInfoStore eventInfoStore) {
		return mapper.valueToTree(eventInfoStore);
	}

	public static String serializeObject(Object object) {
		try {
			return writer.writeValueAsString(mapper.convertValue(object, JsonNode.class));
		} catch (JsonProcessingException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}

	public static JsonNode toJsonNode(final Object object) {
		return mapper.convertValue(object, JsonNode.class);
	}

	public static String serializeJsonNode(JsonNode node) throws JsonProcessingException {
		return writer.writeValueAsString(node);
	}

	// CLONE VIA SERIALIZE -> DESERIALIZE

	// MANIPULATE JSON

	public static String addAttributesModel(String attributesClassName, String json) throws IOException {
		JsonNode node = mapper.readTree(json);
		JsonNode attributesModelNode = node.get("attributesModel");
		DynamicClassInstantiator<Attributes> instantiator = new DynamicClassInstantiator<>();
		((ObjectNode) attributesModelNode).set(
				attributesClassName,
				mapper.convertValue(instantiator.createObject(attributesClassName), JsonNode.class));
		return writer.writeValueAsString(node);
	}
	
	public static JsonNode readTree(String json) throws IOException {
		return mapper.readTree(json);
	}

	public static ObjectNode createObjectNode() {
		return mapper.createObjectNode();
	}
	
	public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
		return mapper.convertValue(fromValue, toValueType);
	}
	
	public static String writeValueAsString(Object value) throws JsonProcessingException {
		return writer.writeValueAsString(value);
	}
}
