package org.vadere.simulator.projects.migration.jsontranformation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.incident.helper.JsonFilterIterator;
import org.vadere.state.attributes.scenario.AttributesMeasurementArea;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.version.Version;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Predicate;

public interface JsonNodeExplorer {


	default ObjectMapper getMapper(){
		return StateJsonConverter.getMapper();
	}

	default void addToObjectNode(JsonNode node, String key, String value) {
		((ObjectNode) node).put(key, value);
	}

	default JsonNode remove(JsonNode root, String childName) throws MigrationException {
		ObjectNode parent = (ObjectNode) root;
		JsonNode removed = parent.remove(childName);
		if (removed == null) {
			throw new MigrationException("Cannot delete childElement '" + childName + "' from parent " + root.asText());
		}
		return removed;
	}

	default void removeIfExists(JsonNode root, String childName) throws MigrationException {
		if (hasChild(root, childName)){
			remove(root, childName);
		}
	}

	default JsonNode setVersionFromTo(JsonNode scenarioFile, Version from, Version to) throws MigrationException {
		JsonNode version = pathMustExist(scenarioFile, "release");
		if (!version.asText().equals(from.label()))
			throw new MigrationException(
					String.format("Previous version is wrong. Expected %s but found %s",
							version.asText(), from.label()));
		((ObjectNode)scenarioFile).put("release", to.label());
		return scenarioFile;
	}

	default void changeStringValue(JsonNode parent, String keyName, String value){
		ObjectNode p = (ObjectNode) parent;
		p.put(keyName, value);
	}

	default void changeIntegerValue(JsonNode parent, String keyName, Integer value){
		ObjectNode p = (ObjectNode) parent;
		p.put(keyName, value);
	}

	default void changeDoubleValue(JsonNode parent, String keyName, double value){
		ObjectNode p = (ObjectNode) parent;
		p.put(keyName, value);
	}

	default  void addIntegerField(JsonNode root, String keyName, int value){
		ObjectNode parent = (ObjectNode) root;
		parent.put(keyName, value);
	}

	default  void addDoubleField(JsonNode root, String keyName, double value){
		ObjectNode parent = (ObjectNode) root;
		parent.put(keyName, value);
	}

	default  void addBooleanField(JsonNode root, String keyName, boolean value){
		ObjectNode parent = (ObjectNode) root;
		parent.put(keyName, value);
	}

	default  void addStringField(JsonNode root, String keyName, String value){
		ObjectNode parent = (ObjectNode) root;
		parent.put(keyName, value);
	}

	default JsonNode pathMustExist(JsonNode root, String path) throws MigrationException {
		JsonNode ret = path(root, path);
		if (ret.isMissingNode()) {
			throw new MigrationException("Json Path Erro: The path '" + path +
					"' should be accessible from " + root.asText());
		}
		return ret;
	}

	default JsonNode path(JsonNode root, String path) {
		String[] pathElements = path.split("/");
		JsonNode ret = root;
		for (String item : pathElements) {
			ret = ret.path(item);
		}
		return ret;
	}

	/**
	 * Test if Node has a child with the given name.
	 * @param root			ParentNode
	 * @param childName		Name of child
	 * @return				True if Parent has a *direct* child named 'childName'
	 */
	default boolean hasChild(JsonNode root, String childName){
		return !path(root, childName).isMissingNode();
	}

	default boolean nodeIsArray(JsonNode node) {
		return nodeNotEmptyAnd(node, n -> n.getNodeType() == JsonNodeType.ARRAY);
	}

	default boolean nodeIsString(JsonNode node){
		return nodeNotEmptyAnd(node, n -> n.getNodeType() == JsonNodeType.STRING);
	}

	default boolean nodeIsNumber(JsonNode node){
		return nodeNotEmptyAnd(node, n -> n.getNodeType() == JsonNodeType.NUMBER);
	}

	default boolean nodeIsBoolean(JsonNode node){
		return nodeNotEmptyAnd(node, n -> n.getNodeType() == JsonNodeType.BOOLEAN);
	}


	default boolean nodeNotEmptyAnd(JsonNode node, Predicate<JsonNode> predicate) {
		return !node.isMissingNode() && predicate.test(node);
	}

	default Iterator<JsonNode> filterIterator(JsonNode root, Predicate<JsonNode> filter) {
		return new Iterator<JsonNode>() {

			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public JsonNode next() {
				return null;
			}
		};
	}

	/**
	 * Create Iterator over all Processors with the given Type.
	 * @param node			root of scenario
	 * @param processorType		type of processors included in the iterator
	 * @return					Iterator of JsonNodes representing a processor of the given type
	 * @throws MigrationException	The scenario file must contain the path processWriters/processors
	 */
	default  Iterator<JsonNode> iteratorProcessorsByType(JsonNode node, String processorType) throws MigrationException {
		JsonNode processors = pathMustExist(node, "processWriters/processors");
		return  new JsonFilterIterator(processors, n -> {
			String type = path(n, "type").asText();
			return type.equals(processorType);
		});
	}

	default Iterator<JsonNode> iteratorTargetChangers(JsonNode node) throws MigrationException{
		JsonNode tChanger = pathMustExist(node, "scenario/topography/targetChangers");
		return new JsonFilterIterator(tChanger, n->true);
	}

	default Iterator<JsonNode> iteratorMeasurementAreas(JsonNode node) throws MigrationException{
		JsonNode tChanger = pathMustExist(node, "scenario/topography/measurementAreas");
		return new JsonFilterIterator(tChanger, n->true);
	}

	default Iterator<JsonNode> iteratorSources(JsonNode node) throws MigrationException{
		JsonNode tChanger = pathMustExist(node, "scenario/topography/sources");
		return new JsonFilterIterator(tChanger, n->true);
	}
	default Iterator<JsonNode> iteratorTargets(JsonNode node) throws MigrationException{
		JsonNode tChanger = pathMustExist(node, "scenario/topography/targets");
		return new JsonFilterIterator(tChanger, n->true);
	}

	default  Iterator<JsonNode> iteratorMeasurementArea(JsonNode node, int id) throws MigrationException {
		JsonNode processors = pathMustExist(node, "scenario/topography/measurementAreas");
		return  new JsonFilterIterator(processors, n -> {
			JsonNode areaId = path(n, "id");
			return !areaId.isMissingNode() && areaId.asInt() == id;
		});
	}

	default void renameField(ObjectNode node, String oldName, String newName){
		JsonNode tmpNode = node.get(oldName);
		node.replace(newName, tmpNode);
		node.remove(oldName);
	}


	default ArrayList<JsonNode> getProcessorsByType(JsonNode node, String processorType) throws MigrationException {
		Iterator<JsonNode> iter = iteratorProcessorsByType(node, processorType);
		ArrayList<JsonNode>  ret = new ArrayList<>();
		while (iter.hasNext()){
			JsonNode n = iter.next();
			ret.add(n);
		}
		return ret;
	}


	default ArrayList<MeasurementArea> deserializeMeasurementArea(JsonNode node, ObjectMapper mapper) throws MigrationException {
		ArrayNode jsonMeasurementArea = (ArrayNode) pathMustExist(node,"scenario/topography/measurementAreas");
		ArrayList<MeasurementArea> measurementAreas = new ArrayList<>();
		Iterator<JsonNode> iter = jsonMeasurementArea.elements();
		while (iter.hasNext()){
			AttributesMeasurementArea attr = null;
			try {
				attr = mapper.readValue(iter.next().toString(), AttributesMeasurementArea.class);
			} catch (IOException e) {
				throw new MigrationException(e.getCause());
			}
			measurementAreas.add(new MeasurementArea(attr));
		}
		return measurementAreas;
	}

	default void createArrayNodeIfNotExist(JsonNode scenarioFile, String relPath, String fieldName) throws MigrationException {
		JsonNode jsonMeasurementArea = path(scenarioFile,relPath + fieldName);
		if (jsonMeasurementArea.isMissingNode()){
			ObjectNode topographyJson = (ObjectNode)pathMustExist(scenarioFile, relPath);
			topographyJson.putArray(fieldName);
		}
	}

	default void addArrayField(JsonNode node, String fieldName,Object data){
		ObjectNode objNode = (ObjectNode)node;
		ArrayNode dataNode = getMapper().valueToTree(data);
		objNode.putArray(fieldName).addAll(dataNode);
	}

	/**
	 *
	 * @param scenarioFile			scenarioFile to migrate
	 * @param shapeNode				Shape used in the {@link org.vadere.simulator.projects.dataprocessing.processor.DataProcessor}
	 * @param mapper				Jackson mapper to serialize and deserialize
	 * @return						The id of the {@link MeasurementArea} to use in the {@link org.vadere.simulator.projects.dataprocessing.processor.DataProcessor}
	 * @throws MigrationException
	 */
	default int transformShapeToMeasurementArea(JsonNode scenarioFile, JsonNode shapeNode, ObjectMapper mapper) throws MigrationException {
		createArrayNodeIfNotExist(scenarioFile, "scenario/topography/", "measurementAreas");
		// get all existing MeasurementAreas.
		ArrayList<MeasurementArea> measurementAreas = deserializeMeasurementArea(scenarioFile, mapper);
		VShape shape = null;
		try {
			shape = mapper.readValue(shapeNode.toString(), VShape.class);
		} catch (IOException e) {
			throw new MigrationException(e.getCause());
		}
		MeasurementArea newArea = new MeasurementArea(new AttributesMeasurementArea(-1, shape));
		// check if an existing MeasurementArea has the same shape.
		// If so use this area. If not add new area to list and update scenarioFile-Json.
		MeasurementArea area = measurementAreas.stream().filter(a -> a.compareByShape(newArea)).findFirst().orElse(newArea);
		if (area == newArea){
			// if a new area is included ensure a unique id. Important: The id is only unique within the MeasurementAreas
			int newId = measurementAreas.stream().map(MeasurementArea::getId).max(Integer::compareTo).orElse(0) + 1;
			area.setId(newId);
			measurementAreas.add(area);
			replaceMeasurementAreas(scenarioFile, measurementAreas, mapper);
		}

		// return id of selected (new or old) MeasurementArea to reference in processor settings.
		return area.getId();
	}

	default void replaceMeasurementAreas(JsonNode scenarioFile, ArrayList<MeasurementArea> measurementAreas, ObjectMapper mapper) throws MigrationException {
		ArrayNode jsonMeasurementArea = (ArrayNode) pathMustExist(scenarioFile,"scenario/topography/measurementAreas");
		jsonMeasurementArea.removeAll();
		for (MeasurementArea area : measurementAreas) {
			JsonNode areaNode = mapper.convertValue(area.getAttributes(), JsonNode.class);
			jsonMeasurementArea.add(areaNode);
		}
	}

	/**
	 * Create Iterator over all files in the scenario which contain the
	 * specified id.
	 * @param node	root of scenario
	 * @param id	id of processor.
	 * @return	Iterator of JsonNodes representing a file.
	 * @throws MigrationException The scenario file must contain the path processWriters/files
	 */
	default Iterator<JsonNode> iteratorFilesForProcessorId(JsonNode node, String id) throws MigrationException {
		JsonNode files = pathMustExist(node, "processWriters/files");
		return new JsonFilterIterator(files, n -> {
			JsonNode processorIds = path(n, "processors");
			if (processorIds.isMissingNode()){
				return false;
			} else {
				for(final JsonNode pId: processorIds){
					boolean ret = pId.asText().equals(id);
					if (ret){
						return true;
					}
				}
				return false;
			}
		});
	}

	default ArrayList<JsonNode> getFilesForProcessorId(JsonNode node, String id) throws MigrationException {
		Iterator<JsonNode> iter = iteratorFilesForProcessorId(node, id);
		ArrayList<JsonNode>  ret = new ArrayList<>();
		while (iter.hasNext()){
			JsonNode n = iter.next();
			ret.add(n);
		}
		return ret;
	}

	default void renameFileFieldName(JsonNode rootNode, String fieldKey, String oldName, String newName) throws MigrationException {
		String childName = "files";
		renameProcessWritersChildField(rootNode, childName, fieldKey, oldName, newName);
	}

	default void renameProcessorFieldName(JsonNode rootNode, String fieldKey, String oldName, String newName) throws MigrationException {
		String childName = "processors";
		renameProcessWritersChildField(rootNode, childName, fieldKey, oldName, newName);
	}

	private void renameProcessWritersChildField(JsonNode rootNode, String childName, String fieldKey, String oldName, String newName) throws MigrationException {
		String path = "processWriters/" + childName;
		JsonNode childArrayNode = pathMustExist(rootNode, path);

		if (childArrayNode.isArray()) {
			for (JsonNode node : childArrayNode) {
				if (node.has(fieldKey)) {
					String fieldValue = node.get(fieldKey).asText("");

					if (fieldValue.contains(oldName)) {
						String newFieldValue = fieldValue.replace(oldName, newName);
						changeStringValue(node, fieldKey, newFieldValue);
					}
				}
			}
		} else {
			throw new MigrationException("Node '" + path + "' should be an Array Node.");
		}
	}

	default void arrayRemoveString(ArrayNode arrayNode, String val){
		Iterator<JsonNode> iter = arrayNode.iterator();
		while (iter.hasNext()){
			JsonNode element = iter.next();
			if (element.asText().equals(val)){
				iter.remove();
			}
		}
	}

	default ArrayList<Double> getDoubleList(JsonNode root, String path) throws MigrationException {
		JsonNode arrList = pathMustExist(root, path);
		ArrayList<Double> dblList = new ArrayList<>();
		if (!arrList.isArray())
			throw new MigrationException("Node should be an Array Node.");
		for (JsonNode n : arrList){
			dblList.add(n.asDouble());
		}
		return  dblList;
	}

	default ArrayList<Integer> getIntegerList(JsonNode root, String path) throws MigrationException {
		JsonNode arrList = pathMustExist(root, path);
		ArrayList<Integer> intList = new ArrayList<>();
		if (!arrList.isArray())
			throw new MigrationException("Node should be an Array Node.");
		for (JsonNode n : arrList){
			intList.add(n.asInt());
		}
		return  intList;
	}


	default ArrayList<String> listOutputFileNames(JsonNode root) throws MigrationException {
		JsonNode files = pathMustExist(root, "processWriters/files");
		ArrayList<String> fileNames = new ArrayList<>();
		for (JsonNode f : files){
			fileNames.add(pathMustExist(f, "filename").asText());
		}
		return fileNames;
	}

	default void createNewOutputFile(JsonNode root, String type, String... pIds) throws MigrationException {
		ArrayNode files = (ArrayNode)pathMustExist(root, "processWriters/files");

		String fileName = "out";
		int i = 1;
		ArrayList<String> fileNames = listOutputFileNames(root);
		while (fileNames.contains(fileName + i + ".txt")){
			i++;
		}
		fileName = fileName + i + ".txt";

		ObjectNode newFile = files.addObject();
		newFile.put("type", type);
		newFile.put("filename", fileName);
		ArrayNode pArr = newFile.putArray("processors");
		for (String pId : pIds) {
			pArr.add(pId);
		}

	}
}
