package org.vadere.simulator.projects.migration.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.incident.incidents.Incident;

/**
 * A Tree that represents the json-tree. Note that JsonArrays are always leafs! Therefore it is
 * not possible to manipulate fields deeper than the first level after of an JsonArray.
 */
public class Tree {

	/**
	 * The root of the Tree
	 */
	private final Node root;

	public Tree(@NotNull final JsonNode rootValue) {
		root = new Node(null, "ROOT", rootValue);
		recursiveBuild(root);
	}

	/**
	 * Converts the json-tree to a {@code Tree} recursively starting at the root.
	 *
	 * @param parent the root of the Tree
	 */
	private void recursiveBuild(@NotNull final Node parent) {
		Iterator<Map.Entry<String, JsonNode>> it = parent.jsonNode.fields();
		while (it.hasNext()) {
			Map.Entry<String, JsonNode> entry = it.next();
			Node child = new Node(parent, entry.getKey(), entry.getValue());
			parent.addChild(child);
			recursiveBuild(child);
		}
	}

	/**
	 * Transform a list of strings (s1, s2, s3, ..., sn) to a string s1 > s2 > ... > sn.
	 * @param path the list of strings
	 * @return the path
	 */
	public static String pathToString(@NotNull final List<String> path) {
		return "[" + path.stream().reduce("", (s1, s2) -> s1 + " > " + s2).substring(path.isEmpty() ? 0 : 3) + "]";
	}

	/**
	 * Returns the path (a list of strings) from the root to the node. The node has to be part of the tree
	 * otherwise this method throws a {@code NullPointerException}.
	 *
	 * @param node the node
	 * @return the path from the root to the node
	 */
	private List<String> getPathToNode(@NotNull Node node) {
		List<String> path = new ArrayList<>();
		while (node != root) {
			path.add(node.key);
			node = node.parent;
		}
		Collections.reverse(path);
		return path;
	}

	/**
	 * TODO: remove caller and log variable!
	 * Tests if the path from the root to the node exists.
	 *
	 * @param path the path
	 * @return true if the path exists, false otherwise
	 */
	public boolean pathExists(@NotNull final List<String> path) {
		return getNodeByPath(path) != null;
	}

	/**
	 * Removes a node identified by its key and its parent key.
	 *
	 * @param parentKey     the key of the parent node
	 * @param key           the key of the node
	 * @param log
	 * @param caller
	 * @throws MigrationException
	 */
	public void deleteUnrecognizedField(final String parentKey, final String key, final StringBuilder log, final Incident caller)
			throws MigrationException {
		List<Node> nodes = recursiveScan(root, parentKey, key, caller);
		if(nodes.size() > 1) {
			throw new MigrationException(caller, "can't automatically deleteEdge the unrecognized field [" + key
					+ "] because more than one tree-path ends with [" + parentKey + " > " + key + "]");
		}
		else if(nodes.isEmpty()) {
			throw new MigrationException(caller, "can't automatically deleteEdge the unrecognized field [" + key
					+ "] no tree-path ends with [" + parentKey + " > " + key + "]");
		}
		else {
			log.append("\t- deleteEdge unrecognized node [" + key + "] under node "
					+ pathToString(getPathToNode(nodes.get(0).parent)) + "\n");
			nodes.get(0).delete();
		}
	}

	/**
	 * TODO: remove caller variable!
	 * Transforms the node to an integer.
	 *
	 * @param parentKey the key of the parent of the node
	 * @param key       the key of the node
	 * @param log       a string builder to protocol the changes
	 * @param caller
	 * @throws MigrationException
	 */
	public void enforceIntegerValue(String parentKey, String key, StringBuilder log, Incident caller)
			throws MigrationException {
		List<Node> nodes = recursiveScan(root, parentKey, key, caller);

		if(nodes.size() > 1) {
			throw new MigrationException(caller, "can't automatically enforce integer-value for [" + key
					+ "] because more than one tree-path ends with [" + parentKey + " > " + key + "]");
		}
		else if(nodes.isEmpty()) {
			throw new MigrationException(caller, "can't automatically enforce integer-value for [" + key
					+ "] no tree-path ends with [" + parentKey + " > " + key + "]");
		}
		else {
			log.append("\t- enforce integer-value (" + nodes.get(0).jsonNode.asInt() + " instead of " + nodes.get(0).jsonNode
					+ ") of node [" + key + "] under node " + pathToString(getPathToNode(nodes.get(0).parent)) + "\n");
			nodes.get(0).enforceIntegerValue();
		}
	}

	/**
	 * Searches for a all nodes which has to be the child of a node which has the key equal to parentKey.
	 * The search starts at parentCandidate in a dept-first-fashion.
	 *
	 * @param parentCandidate   the current parent candidate
	 * @param parentKey         the key of the parent
	 * @param key               the key of the node
	 * @param caller
	 * @throws MigrationException
	 */
	public List<Node> recursiveScan(final Node parentCandidate, final String parentKey, final String key, final Incident caller)
			throws MigrationException { // the alternative would be to collect all leaves and check their parents
		List<Node> foundNodes = new LinkedList<>();
		recursiveScan(parentCandidate, parentKey, key, foundNodes);
		return foundNodes;
	}

	private void recursiveScan(final Node parentCandidate, final String parentKey, final String key, final List<Node> foundNodes) {
		if (parentCandidate.key.equals(parentKey) && parentCandidate.children.keySet().contains(key)) { // = a path was found that ends on [childKey] and has [parentKey] as parent
			foundNodes.add(parentCandidate.children.get(key));
		}

		for (String child : parentCandidate.children.keySet()) {
			recursiveScan(parentCandidate.children.get(child), parentKey, key, foundNodes);
		}
	}

	/**
	 * Tests if a key is present in an first level {@code JsonArray}.
	 *
	 * @param pathToArray   the path to the {@code JsonArray}
	 * @param key           the name/key the method tests for
	 * @return true if the key is present in the {@code JsonArray}, otherwise false
	 */
	public boolean keyExistsInArray(final List<String> pathToArray, final String key) {
		Node arrayNode = getNodeByPath(pathToArray);

		// the array does not exist
		if (arrayNode == null)
			return false;

		for (JsonNode entry : arrayNode.jsonNode)
			if (entry.has(key))
				return true;

		return false;
	}

	/**
	 * Returns the node identified by the path starting from the root or {@code null} if
	 * the path does not lead to a node.
	 *
	 * @param path the path to the node starting from the root
	 * @return the node identified by the path or null
	 */
	public Node getNodeByPath(@NotNull final List<String> path) {
		if (path.isEmpty()) {
			return root;
		}
		Node descend = root;
		int index = 0;
		while (descend != null && index < path.size())
			descend = descend.children.get(path.get(index++));
		return descend;
	}

	/**
	 * Takes a node identified by a path, removes the node from its location and adds
	 * it to a node identified by another path.
	 *
	 * @param fullOldPath   the path that identifies the node which will be relocated
	 * @param newPath       the path that identfies the new parent of the relocated node
	 */
	public void relocateNode(List<String> fullOldPath, List<String> newPath) {
		Node node = getNodeByPath(fullOldPath);
		Node newParent = getNodeByPath(newPath);
		node.relocateTo(newParent);
	}

	/**
	 * Deletes a node. The node has to exist.
	 *
	 * @param path the path to the node
	 */
	public void deleteNode(@NotNull final List<String> path) {
		getNodeByPath(path).delete();
	}

	/**
	 * Renames a node. The node has to exist.
	 *
	 * @param path      the path to the node
	 * @param newName   the new name/key of the node
	 */
	public void renameNode(@NotNull final List<String> path, @NotNull final String newName) {
		getNodeByPath(path).renameTo(newName);
	}

	/**
	 * Renames a JsonObject of an JsonArray. This object has to be a child of the array and it has to be a JsonObject and
	 * not a JsonArray.
	 *
	 * @param pathToArray   path to the array (can not contain any array in between)
	 * @param oldName       the old name/key of the JsonObject
	 * @param newName       the new name/key of the JsonObject
	 */
	public void renameKeyOccurrencesInArray(@NotNull final List<String> pathToArray, final @NotNull String oldName, @NotNull final String newName) {
		for (JsonNode entry : getNodeByPath(pathToArray).jsonNode) {
			if (entry.has(oldName)) {
				JsonNode value = entry.get(oldName);
				((ObjectNode) entry).remove(oldName);
				((ObjectNode) entry).set(newName, value); // will be added as last element, not same index as oldName was unfortunately
			}
		}
	}

	/**
	 * Deletes a JsonObject which is a child of a JsonArray. This object has to be a child of the array and it has to be a JsonObject and
	 * not a JsonArray.
	 *
	 * @param pathToArray   path to the array (can not contain any array in between)
	 * @param keyName       the name/key of the JsonObject
	 */
	public void deleteNodeInArray(@NotNull final List<String> pathToArray, @NotNull final String keyName) {
		for(JsonNode entry : getNodeByPath(pathToArray).jsonNode) {
			if (entry.has(keyName)) {
				((ObjectNode) entry).remove(keyName);
			}
		}
	}

	/**
	 * Returns all names/keys of the children of a node. This node has to exist.
	 *
	 * @param path  the path to the node
	 * @return all names/keys of the children of a node
	 */
	public Set<String> getKeysOfChildren(@NotNull final List<String> path) {
		return getNodeByPath(path).children.keySet();
	}

	/**
	 * Inserts a new text-node at the node identified by the path. The node has to exist and it as to be an {@code ObjectNode}.
	 *
	 * @param path  the path to the node
	 * @param key   the key of the inserted text-node
	 * @param value the text of the inserted text-node
	 */
	public void createTextNode(@NotNull final List<String> path, @NotNull final String key, final String value) {
		((ObjectNode) getNodeByPath(path).jsonNode).put(key, value);
	}

	public Node getRoot() {
		return root;
	}

	public class Node { // wrapper for JsonNode that adds key, parent and children for traversing the graph

		Node parent;
		Map<String, Node> children = new HashMap<>(); // no duplicate keys allowed in one json node

		String key;
		JsonNode jsonNode;

		public JsonNode getJsonNode() {
			return jsonNode;
		}

		/**
		 * Creates a new node.
		 * @param parent    the parent of the node
		 * @param key       the key of the node i.e. the name of the json-object
		 * @param jsonNode  the json-node i.e. the json-object which this node represents
		 */
		public Node(@Nullable final Node parent, @NotNull final String key, @NotNull final JsonNode jsonNode) {
			this.parent = parent;
			this.key = key;
			this.jsonNode = jsonNode;
		}

		/**
		 * Adds a child to the node
		 *
		 * @param child
		 */
		public void addChild(final @NotNull Node child) {
			children.put(child.key, child);
		}

		/**
		 * Removes the node from its parent and adds it as a child to a new parent.
		 * This operation affects the tree and the json-tree.
		 *
		 * @param newParent the new parent
		 */
		public void relocateTo(final @NotNull Node newParent) {
			parent.children.remove(key);
			newParent.addChild(this);
			((ObjectNode) parent.jsonNode).remove(key);
			((ObjectNode) newParent.jsonNode).set(key, jsonNode);
			parent = newParent;
		}

		/**
		 * Deletes the node from its parent.
		 * This operation affects the tree and the json-tree.
		 */
		public void delete() {
			parent.children.remove(key);
			((ObjectNode) parent.jsonNode).remove(key);
		}

		/**
		 * Updates the key of the node.
		 * This operation affects the tree and the json-tree.
		 *
		 * @param newKey the new key
		 */
		public void renameTo(final @NotNull String newKey) {
			parent.children.remove(key);
			parent.children.put(newKey, this);
			((ObjectNode) parent.jsonNode).remove(key);
			((ObjectNode) parent.jsonNode).set(newKey, jsonNode);
			key = newKey;
		}

		/**
		 * Converts the jsonNode of the node to an integer and replaces the old value by this integer.
		 * Booleans convert to 0 (false) and 1 (true), and Strings are parsed using default Java language integer
		 * parsing rules. If representation can not be converted to an int (including structured types
		 * like Objects and Arrays), default value is 0; no exceptions are thrown.
		 *
		 */
		public void enforceIntegerValue() {
			ObjectNode parentObjectNode = ((ObjectNode) parent.jsonNode);
			parentObjectNode.remove(key);
			parentObjectNode.put(key, jsonNode.asInt());
			jsonNode = parentObjectNode.get(key); // to keep the graph correct
		}

		public String getKey() {
			return key;
		}
	}

}
