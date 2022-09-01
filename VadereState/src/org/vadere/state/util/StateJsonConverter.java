package org.vadere.state.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.vadere.state.attributes.*;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.*;
import org.vadere.state.psychology.perception.json.StimulusInfo;
import org.vadere.state.psychology.perception.json.StimulusInfoStore;
import org.vadere.state.scenario.*;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.logging.Logger;
import org.vadere.util.reflection.DynamicClassInstantiator;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class StateJsonConverter {

	public static final String SCENARIO_KEY = "scenario";

	public static final String MAIN_MODEL_KEY = "mainModel";

	private static final String ATTRIBUTES_MODEL_KEY = "attributesModel";

	private static final String PSYCHOLOGY_LAYER_KEY = "psychologyLayer";
	

	private static final TypeReference<Map<String, Object>> mapTypeReference =
			new TypeReference<Map<String, Object>>() {};

	private static Logger logger = Logger.getLogger(StateJsonConverter.class);

	/** Connection to jackson library. */
	private static ObjectMapper mapper = new JacksonObjectMapper();

	/** Connection to jackson library. */
	private static ObjectWriter prettyWriter = mapper.writerWithDefaultPrettyPrinter();

	private static ObjectWriter writer = mapper.writer();

	public static ObjectMapper getMapper() {
		return mapper;
	}

	public static ObjectWriter getPrettyWriter() {
		return prettyWriter;
	}

	public static <T> T deserializeObjectFromJson(String json, Class<T> objectClass) throws IOException {
		final JsonNode node = mapper.readTree(json);
		checkForTextOutOfNode(json);
		return mapper.treeToValue(node, objectClass);
	}

	public static <T> T deserializeObjectFromJson(String json, final TypeReference<T> type) {
		T data = null;

		try {
			data = mapper.readValue(json, type);
		} catch (Exception e) {
			e.printStackTrace();
			// Handle the problem
		}
		return data;
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
		Collection<AttributesTargetChanger> targetChangers = new LinkedList<>();
		Collection<AttributesAbsorbingArea> absorbingAreas = new LinkedList<>();
		Collection<AttributesAerosolCloud> aerosolClouds = new LinkedList<>();
		Collection<AttributesDroplets> droplets = new LinkedList<>();
		Collection<AttributesSource> sources = new LinkedList<>();
		Collection<AttributesMeasurementArea> measurementAreas = new LinkedList<>();
		Collection<? extends DynamicElement> dynamicElements = new LinkedList<>();
		AttributesTeleporter teleporter = null;
	}

	public static AttributesSimulation deserializeAttributesSimulation(String json) throws IOException  {
		return deserializeObjectFromJson(json, AttributesSimulation.class);
	}

	public static AttributesSimulation deserializeAttributesSimulationFromNode(JsonNode node)
			throws JsonProcessingException {
		return mapper.treeToValue(node, AttributesSimulation.class);
	}

	public static AttributesPsychology deserializeAttributesPsychology(String json) throws IOException  {
		return deserializeAttributesPsychologyFromNode(mapper.readTree(json));
	}

	public static AttributesPsychology deserializeAttributesPsychologyFromNode(JsonNode jsonNode)
			throws JsonProcessingException {

		ObjectNode node = jsonNode.deepCopy();
		JsonNode layer = node.get(PSYCHOLOGY_LAYER_KEY);
		node.remove(PSYCHOLOGY_LAYER_KEY);

		AttributesPsychology attributesPsychology = mapper.treeToValue(node, AttributesPsychology.class);
		attributesPsychology.setPsychologyLayer(deseralizeAttributesPsychologyLayerFromNode(layer));

		return attributesPsychology;
	}

	private static AttributesPsychologyLayer deseralizeAttributesPsychologyLayerFromNode(JsonNode jsonNode) throws JsonProcessingException {

		ObjectNode node = jsonNode.deepCopy();
		JsonNode attributesModel = node.get(ATTRIBUTES_MODEL_KEY);
		node.remove(ATTRIBUTES_MODEL_KEY);

		AttributesPsychologyLayer layer = mapper.treeToValue(node, AttributesPsychologyLayer.class);
		layer.setAttributesModel(deserializeAttributesListFromNode(attributesModel));

		return layer;
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

	public static Topography deserializeTopography(String json) throws IOException {
		checkForTextOutOfNode(json);
		return deserializeTopographyFromNode(mapper.readTree(json));
	}

	public static Topography deserializeTopographyFromNode(JsonNode node) throws IllegalArgumentException {
		TopographyStore store = mapper.convertValue(node, TopographyStore.class);
		Topography topography = new Topography(store.attributes, store.attributesPedestrian, store.attributesCar);
		store.obstacles.forEach(obstacle -> topography.addObstacle(new Obstacle(obstacle)));
		store.stairs.forEach(stairs -> topography.addStairs(new Stairs(stairs)));
		store.targets.forEach(target -> topography.addTarget(new Target(target)));
		store.targetChangers.forEach(targetChanger -> topography.addTargetChanger(new TargetChanger(targetChanger)));
		store.absorbingAreas.forEach(absorbingArea -> topography.addAbsorbingArea(new AbsorbingArea(absorbingArea)));
		store.sources.forEach(source -> topography.addSource(new Source(source)));
		store.measurementAreas.forEach(area -> topography.addMeasurementArea(new MeasurementArea(area)));
		store.aerosolClouds.forEach(aerosolCloud -> topography.addAerosolCloud(new AerosolCloud(aerosolCloud)));
		store.droplets.forEach(droplets -> topography.addDroplets(new Droplets(droplets)));

		store.dynamicElements.forEach(topography::addInitialElement);

		if (store.teleporter != null)
			topography.setTeleporter(new Teleporter(store.teleporter));

		return topography;
	}

	public static void checkForTextOutOfNode(String json) throws IOException {
		// via stackoverflow.com/a/26026359
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
	 * Pass a node representing an array of @see StimulusInfo objects.
	 *
	 * Usually, this array is extracted by reading in a scenario file as @see JsonNode
	 * and you call "get("stimulusInfos") on this @see JsonNode.
	 */
	public static StimulusInfoStore deserializeStimuliFromArrayNode(JsonNode nodef) throws IllegalArgumentException {
		StimulusInfoStore stimulusInfoStore = new StimulusInfoStore();

		if (nodef != null) {
			JsonNode node;
			String key = "stimulusInfos";
			if (nodef.has(key)) {
				node = nodef.get(key);
				List<StimulusInfo> stimulusInfoList = new ArrayList<>();
				node.forEach(stimulusInfoNode -> stimulusInfoList.add(mapper.convertValue(stimulusInfoNode, StimulusInfo.class)));
				stimulusInfoStore.setStimulusInfos(stimulusInfoList);
			}
		}

		return stimulusInfoStore;
	}

	public static StimulusInfo deserializeStimulusInfo(String json) throws IOException {
		return mapper.readValue(json, StimulusInfo.class);
	}





	public static StimulusInfoStore deserializeStimuli(String json) throws IOException {
		return mapper.readValue(json, StimulusInfoStore.class);
	}

	public static Pedestrian deserializePedestrian(String json) throws IOException {
		/*mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		JsonNode jsonNodeRoot = mapper.readTree(json);
		JsonNode jsonNodeGroupId = jsonNodeRoot.get("groupId");*/
		Pedestrian ped = mapper.readValue(json, Pedestrian.class);
		/*if (jsonNodeGroupId != null) {
			ped.registerGroupAccess();
		}
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);*/
		return ped;
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
			case TARGET_CHANGER:
				return mapper.readValue(json, AttributesTargetChanger.class);
			case ABSORBING_AREA:
				return mapper.readValue(json, AttributesAbsorbingArea.class);
			case STAIRS:
				return mapper.readValue(json, AttributesStairs.class);
			case MEASUREMENT_AREA:
				return mapper.readValue(json, AttributesMeasurementArea.class);
			case AEROSOL_CLOUD:
				return mapper.readValue(json, AttributesAerosolCloud.class);
			case DROPLETS:
				return mapper.readValue(json, AttributesDroplets.class);
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
		node.set(ATTRIBUTES_MODEL_KEY, serializeAttributesModelToNode(modelDefinition.getAttributesList()));
		return prettyWriter.writeValueAsString(node);
	}

	public static ObjectNode serializeAttributesModelToNode(final Attributes... attributesList) {
		return serializeAttributesModelToNode(Arrays.stream(attributesList).collect(Collectors.toList()));
	}

	public static ObjectNode serializeAttributesModelToNode(final List<Attributes> attributesList) {
		List<Pair<String, Attributes>> attributePairList = attributesListToNameObjectPairList(attributesList);

		ObjectNode attributesModelNode = mapper.createObjectNode();
		attributePairList.forEach(pair -> attributesModelNode.set(pair.getLeft(),
				mapper.convertValue(pair.getRight(), JsonNode.class)));

		return attributesModelNode;
	}

	private static List<Pair<String, Attributes>> attributesListToNameObjectPairList(Attributes... attributesList) {
		return attributesListToNameObjectPairList(Arrays.stream(attributesList).collect(Collectors.toList()));
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

		ArrayNode measurementAreaNodes = mapper.createArrayNode();
		topography.getMeasurementAreas()
				.forEach(area -> measurementAreaNodes.add(mapper.convertValue(area.getAttributes(), JsonNode.class)));
		topographyNode.set("measurementAreas", measurementAreaNodes);

		ArrayNode stairNodes = mapper.createArrayNode();
		topography.getStairs()
				.forEach(stair -> stairNodes.add(mapper.convertValue(stair.getAttributes(), JsonNode.class)));
		topographyNode.set("stairs", stairNodes);

		ArrayNode targetNodes = mapper.createArrayNode();
		topography.getTargets()
				.forEach(target -> targetNodes.add(mapper.convertValue(target.getAttributes(), JsonNode.class)));
		topographyNode.set("targets", targetNodes);

		ArrayNode targetChangerNodes = mapper.createArrayNode();
		topography.getTargetChangers()
				.forEach(targetChanger -> targetChangerNodes.add(mapper.convertValue(targetChanger.getAttributes(), JsonNode.class)));
		topographyNode.set("targetChangers", targetChangerNodes);

		ArrayNode absorbingAreaNodes = mapper.createArrayNode();
		topography.getAbsorbingAreas()
				.forEach(absorbingArea -> absorbingAreaNodes.add(mapper.convertValue(absorbingArea.getAttributes(), JsonNode.class)));
		topographyNode.set("absorbingAreas", absorbingAreaNodes);

		ArrayNode aerosolCloudNodes = mapper.createArrayNode();
		topography.getAerosolClouds()
				.forEach(aerosolCloud -> aerosolCloudNodes.add(mapper.convertValue(aerosolCloud.getAttributes(), JsonNode.class)));
		topographyNode.set("aerosolClouds", aerosolCloudNodes);

		ArrayNode dropletsNodes = mapper.createArrayNode();
		topography.getDroplets()
				.forEach(droplets -> dropletsNodes.add(mapper.convertValue(droplets.getAttributes(), JsonNode.class)));
		topographyNode.set("droplets", dropletsNodes);

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

		AttributesTeleporter attributesTeleporter = null;
		if(topography.getTeleporter() != null) {
			attributesTeleporter = topography.getTeleporter().getAttributes();
		}
		JsonNode node = mapper.convertValue(attributesTeleporter, JsonNode.class);
		topographyNode.set("teleporter", node);

		JsonNode attributesCarNode = mapper.convertValue(topography.getAttributesCar(), JsonNode.class);
		topographyNode.set("attributesCar", attributesCarNode);

		return topographyNode;
	}

	public static String serializeAttributesSimulation(AttributesSimulation attributesSimulation)
			throws JsonProcessingException {
		return prettyWriter.writeValueAsString(mapper.convertValue(attributesSimulation, JsonNode.class));
	}

	public static String serializeAttributesPsychology(AttributesPsychology attributesPsychology)
			throws JsonProcessingException {

		ObjectNode node = serializeAttributesPsychologyToNode(attributesPsychology);
		return prettyWriter.writeValueAsString(node);
	}

	public static ObjectNode serializeAttributesPsychologyToNode(AttributesPsychology attributesPsychology) {
		ObjectNode node = mapper.valueToTree(attributesPsychology);
		ObjectNode psychologyLayer = (ObjectNode) node.get(PSYCHOLOGY_LAYER_KEY);
		ObjectNode attributesModel = serializeAttributesModelToNode(attributesPsychology.getPsychologyLayer().getAttributesModel());
		psychologyLayer.put(ATTRIBUTES_MODEL_KEY, attributesModel);
		return node;
	}

	public static String serializeAttributesStrategyModel(AttributesStrategyModel attributesStrategyModel)
			throws JsonProcessingException {
		return prettyWriter.writeValueAsString(mapper.convertValue(attributesStrategyModel, JsonNode.class));
	}

	public static String serializeTopography(Topography topography) throws JsonProcessingException {
		return prettyWriter.writeValueAsString(serializeTopographyToNode(topography));
	}

	public static String serializeMainModelAttributesModelBundle(List<Attributes> attributesList, String mainModel)
			throws JsonProcessingException {
		ObjectNode node = mapper.createObjectNode();
		node.put(MAIN_MODEL_KEY, mainModel);
		node.set(ATTRIBUTES_MODEL_KEY, serializeAttributesModelToNode(attributesList));
		return prettyWriter.writeValueAsString(node);
	}

	public static String serializeStimuli(StimulusInfoStore stimulusInfoStore)
			throws JsonProcessingException {
		return prettyWriter.writeValueAsString(mapper.convertValue(stimulusInfoStore, JsonNode.class));
	}

	public static ObjectNode serializeStimuliToNode(StimulusInfoStore stimulusInfoStore) {
		return mapper.valueToTree(stimulusInfoStore);
	}


	public static ObjectNode serializeAttributesStrategyModelToNode(AttributesStrategyModel attributesStrategyModel) {
		return mapper.valueToTree(attributesStrategyModel);
	}


	public static String serializeObjectPretty(Object object) {
		try {
			return prettyWriter.writeValueAsString(mapper.convertValue(object, JsonNode.class));
		} catch (JsonProcessingException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}

	public static String serializeObject(Object object) {
		try {
			return writer.writeValueAsString(mapper.convertValue(object, JsonNode.class));
		} catch (JsonProcessingException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getScenarioStoreHash(Object object){
		JsonNode jsonNode = mapper.convertValue(object, JsonNode.class);
		JsonNode attrSimulation = jsonNode.findPath(AttributesSimulation.JSON_KEY);
		if (! attrSimulation.isMissingNode()){
			((ObjectNode)attrSimulation).remove("simulationSeed");
			((ObjectNode)attrSimulation).remove("useFixedSeed");
			((ObjectNode)attrSimulation).remove("simulationSeed");
		}

		try {
			String scenarioString = prettyWriter.writeValueAsString(jsonNode);
			return DigestUtils.sha1Hex(scenarioString);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static JsonNode toJsonNode(final Object object) {
		return mapper.convertValue(object, JsonNode.class);
	}

	public static String serializeJsonNode(JsonNode node) throws JsonProcessingException {
		return prettyWriter.writeValueAsString(node);
	}

	// CLONE VIA SERIALIZE -> DESERIALIZE

	// MANIPULATE JSON

	public static String addAttributesModel(String attributesClassName, String json) throws IOException {
		JsonNode node = mapper.readTree(json);
		JsonNode attributesModelNode = node.get(ATTRIBUTES_MODEL_KEY);
		DynamicClassInstantiator<Attributes> instantiator = new DynamicClassInstantiator<>();
		((ObjectNode) attributesModelNode).set(
				attributesClassName,
				mapper.convertValue(instantiator.createObject(attributesClassName), JsonNode.class));
		return prettyWriter.writeValueAsString(node);
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
		return prettyWriter.writeValueAsString(value);
	}

	/**
	 * Create a SHA-1 hash based on the given {@link AttributesFloorField} and {@link Topography}.
	 * Use the Jackson view {@link Views.CacheView} to EXCLUDE the @link AttributesFloorField#cacheDir
	 * field to allow reallocation of created floor field caches.
	 *
	 */
	public static String getFloorFieldHash(final Topography topography, final AttributesFloorField attr)  {
		try {
			String topographyStr = mapper
									.writerWithDefaultPrettyPrinter()
									.withView(Views.CacheView.class)
									.writeValueAsString(topography);
			String attrString = mapper
									.writerWithDefaultPrettyPrinter()
									.withView(Views.CacheView.class)
									.writeValueAsString(attr);
			String hashIt = attrString + "\n" + topographyStr;
			String hash = DigestUtils.sha1Hex(hashIt.getBytes());
			logger.debugf("created Hash: %s", hash);
			logger.tracef("used String for hash: \n%s", hashIt);
			return hash;
		} catch (JsonProcessingException e) {
			logger.error("cannot create hash of topography and floor field attributes for cache access.");
		}
		return DigestUtils.sha1Hex("error");
	}
}
