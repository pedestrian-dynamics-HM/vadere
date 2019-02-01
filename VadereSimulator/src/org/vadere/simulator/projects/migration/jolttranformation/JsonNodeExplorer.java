package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.helper.JsonFilterIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Predicate;

public interface JsonNodeExplorer {



	default void addToObjectNode(JsonNode node, String key, String value) {
		((ObjectNode) node).put(key, value);
	}

	default void remove(JsonNode root, String childName) throws MigrationException {
		ObjectNode parent = (ObjectNode) root;
		if (parent.remove(childName) == null) {
			throw new MigrationException("Cannot delete childElement '" + childName + "' from parent " + root.asText());
		}
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

	default boolean nodeIsArray(JsonNode node) {
		return nodeNotEmptyAnd(node, n -> n.getNodeType() == JsonNodeType.ARRAY);
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

	default void arrayRemoveString(ArrayNode arrayNode, String val){
		Iterator<JsonNode> iter = arrayNode.iterator();
		while (iter.hasNext()){
			JsonNode element = iter.next();
			if (element.asText().equals(val)){
				iter.remove();
			}
		}
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
