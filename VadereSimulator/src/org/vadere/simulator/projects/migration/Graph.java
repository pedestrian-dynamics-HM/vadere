package org.vadere.simulator.projects.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

import org.vadere.simulator.projects.migration.incidents.Incident;

public class Graph {

	private final Node root;
	private Node candidate;

	public Graph(JsonNode rootValue) {
		root = new Node(null, "ROOT", rootValue);
		recursiveBuild(root);
	}

	public void recursiveBuild(Node parent) {
		Iterator<Map.Entry<String, JsonNode>> it = parent.jsonNode.fields();
		while (it.hasNext()) {
			Map.Entry<String, JsonNode> entry = it.next();
			Node child = new Node(parent, entry.getKey(), entry.getValue());
			parent.addChild(child);
			recursiveBuild(child);
		}
	}

	public String pathToString(List<String> path) {
		if (path.size() > 0) {
			StringBuilder sb = new StringBuilder();
			path.forEach(node -> sb.append(" > " + node));
			return "[" + sb.toString().substring(3) + "]";
		}
		return "[]";
	}

	private List<String> getPathToNode(Node node) {
		List<String> path = new ArrayList<>();
		while (node != root) {
			path.add(node.key);
			node = node.parent;
		}
		Collections.reverse(path);
		return path;
	}

	public boolean pathExists(List<String> path) {
		return getNodeByPath(path) != null;
	}

	public void deleteUnrecognizedField(String parentKey, String childKey, StringBuilder log, Incident caller)
			throws MigrationException {
		recursiveScan(root, parentKey, childKey, caller);
		log.append("\t- delete unrecognized node [" + childKey + "] under node "
				+ pathToString(getPathToNode(candidate.parent)) + "\n");
		candidate.delete();
		candidate = null;
	}

	public void enforceIntegerValue(String parentKey, String childKey, StringBuilder log, Incident caller)
			throws MigrationException {
		recursiveScan(root, parentKey, childKey, caller);
		log.append("\t- enforce integer-value (" + candidate.jsonNode.asInt() + " instead of " + candidate.jsonNode
				+ ") of node [" + childKey + "] under node " + pathToString(getPathToNode(candidate.parent)) + "\n");
		candidate.enforceIntegerValue();
		candidate = null;
	}

	public void recursiveScan(Node parent, String parentKey, String childKey, Incident caller)
			throws MigrationException { // the alternative would be to collect all leaves and check their parents
		if (parent.key.equals(parentKey) && parent.children.keySet().contains(childKey)) { // = a path was found that ends on [childKey] and has [parentKey] as parent
			if (candidate == null) {
				candidate = parent.children.get(childKey);
			} else {
				throw new MigrationException(caller, "can't automatically delete the unrecognized field [" + childKey
						+ "] because more than one graph-path ends with [" + parentKey + " > " + childKey + "]");
			}
		}
		for (String child : parent.children.keySet())
			recursiveScan(parent.children.get(child), parentKey, childKey, caller);
	}

	public boolean keyExistsInArray(List<String> pathToArray, String oldName) {
		Node arrayNode = getNodeByPath(pathToArray);

		if (arrayNode == null)
			return false;

		for (JsonNode entry : arrayNode.jsonNode)
			if (entry.has(oldName))
				return true;

		return false;
	}

	public Node getNodeByPath(List<String> path) {
		if (path.isEmpty()) {
			return root;
		}
		Node descend = root;
		int index = 0;
		while (descend != null && index < path.size())
			descend = descend.children.get(path.get(index++));
		return descend;
	}

	public void relocateNode(List<String> fullOldPath, List<String> newPath) {
		Node node = getNodeByPath(fullOldPath);
		Node newParent = getNodeByPath(newPath);
		node.relocateTo(newParent);
	}

	public void deleteNode(List<String> path) {
		getNodeByPath(path).delete();
	}

	public void renameNode(List<String> path, String newName) {
		getNodeByPath(path).renameTo(newName);
	}

	public void renameKeyOccurrencesInArray(List<String> pathToArray, String oldName, String newName) {
		for (JsonNode entry : getNodeByPath(pathToArray).jsonNode) {
			if (entry.has(oldName)) {
				JsonNode value = entry.get(oldName);
				((ObjectNode) entry).remove(oldName);
				((ObjectNode) entry).set(newName, value); // will be added as last element, not same index as oldName was unfortunately
			}
		}
	}

	public Set<String> getKeysOfChildren(List<String> path) {
		return getNodeByPath(path).children.keySet();
	}

	public void createTextNode(List<String> path, String key, String value) {
		((ObjectNode) getNodeByPath(path).jsonNode).put(key, value);
	}


	public class Node { // wrapper for JsonNode that adds key, parent and children for traversing the graph

		Node parent;
		Map<String, Node> children = new HashMap<>(); // no duplicate keys allowed in one json node

		String key;
		JsonNode jsonNode;

		public JsonNode getJsonNode() {
			return jsonNode;
		}

		public Node(Node parent, String key, JsonNode jsonNode) {
			this.parent = parent;
			this.key = key;
			this.jsonNode = jsonNode;
		}

		public void addChild(Node child) {
			children.put(child.key, child);
		}

		public void relocateTo(Node newParent) {
			parent.children.remove(key);
			newParent.addChild(this);
			((ObjectNode) parent.jsonNode).remove(key);
			((ObjectNode) newParent.jsonNode).set(key, jsonNode);
			parent = newParent;
		}

		public void delete() {
			parent.children.remove(key);
			((ObjectNode) parent.jsonNode).remove(key);
		}

		public void renameTo(String newKey) {
			parent.children.remove(key);
			parent.children.put(newKey, this);
			((ObjectNode) parent.jsonNode).remove(key);
			((ObjectNode) parent.jsonNode).set(newKey, jsonNode);
			key = newKey;
		}

		public void enforceIntegerValue() {
			ObjectNode parentObjectNode = ((ObjectNode) parent.jsonNode);
			parentObjectNode.remove(key);
			parentObjectNode.put(key, jsonNode.asInt());
			jsonNode = parentObjectNode.get(key); // to keep the graph correct
		}
	}

}
