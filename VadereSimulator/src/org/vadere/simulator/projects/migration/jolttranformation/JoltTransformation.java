package org.vadere.simulator.projects.migration.jolttranformation;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.Diffy;
import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.log4j.Logger;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jolttranformation.JoltTransformationFactory;
import org.vadere.state.util.StateJsonConverter;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;


public abstract class JoltTransformation {

	protected final static Logger logger = Logger.getLogger(JoltTransformation.class);
	private static ConcurrentHashMap<Version, JoltTransformation> transformations = new ConcurrentHashMap<>();

	private final Chainr chainr; // Transformation from (version -1) to version
	private final Chainr identity; // Identity of version
	private final Diffy diffy;
	protected ArrayList<PostTransformHook> postTransformHooks;


	public static JoltTransformation get(Version currentVersion) throws MigrationException {
		if (currentVersion.equalOrSamller(Version.UNDEFINED))
			throw new MigrationException("There is now Transformation for Version " + Version.UNDEFINED.toString());

		if (currentVersion.equals(Version.latest()))
			throw new MigrationException("No Transformation needed. Already latest Version!");


		JoltTransformationFactory factory = JoltTransformationFactory.instance();
		JoltTransformation ret;
		try {
			ret = factory.getInstanceOf(currentVersion.nextVersion().label('_'));
		} catch (ClassNotFoundException e) {
			throw new MigrationException("Cannot find Transformation in Factory for Version " + currentVersion.nextVersion().label());
		}
		return ret;
	}

	public static Path getTransforamtionFile(Version toVersion) {
		String transformString = getTransforamtionResourcePath(
				toVersion.previousVersion().label('-'),
				toVersion.label('-'));
		URI res;
		try {
			res = JoltTransformation.class.getResource(transformString).toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException("Cannot find transformation file for version " + toVersion.label());
		}
		return Paths.get(res);
	}

	public static Path getIdenityFile(Version v) {
		String idenityString = getIdentiyResoucrePath(v.label('-'));
		URI res = null;
		try {
			res = JoltTransformation.class.getResource(idenityString).toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException("Cannot find identity file for version" + v.label());
		}
		return Paths.get(res);
	}

	public static String getTransforamtionResourcePath(String from, String to) {
		return "/transform_v" + from.toUpperCase() + "_to_v" + to.toUpperCase() + ".json";
	}

	public static String getIdentiyResoucrePath(String to) {
		return "/identity_v" + to.toUpperCase() + ".json";
	}

	public JoltTransformation(String transformation, String identity) {
		this.chainr = Chainr.fromSpec(JsonUtils.classpathToList(transformation));
		this.identity = Chainr.fromSpec(JsonUtils.classpathToList(identity));
		// Output of Transformation
		this.postTransformHooks = new ArrayList<>();
		this.diffy = new Diffy();
		initPostHooks();
	}

	public JoltTransformation(Version targetVersion) {
		this(getTransforamtionResourcePath(
				targetVersion.previousVersion().label('-'),
				targetVersion.label('-')),
				getIdentiyResoucrePath(targetVersion.label('-')));
	}

	public JsonNode applyTransformation(JsonNode root) throws MigrationException {
		Object rootObject = StateJsonConverter.convertJsonNodeToObject(root);
		rootObject = applyTransformation(rootObject);
		JsonNode jsonRoot = StateJsonConverter.deserializeToNode(rootObject);
		return applyPostHooks(jsonRoot);
	}

	private Object applyTransformation(Object root) throws MigrationException {
		Object rootTransformed = chainr.transform(root);
		Object rootTransformedIdenity = identity.transform(rootTransformed);

		Diffy.Result diffResult = diffy.diff(rootTransformed, rootTransformedIdenity);
		if (diffResult.isEmpty()) {
			return rootTransformed;
		} else {
			logger.error("Error in Transformation " + diffResult.toString());
			throw new MigrationException("Error in Transformation " + diffResult.toString());
		}
	}

	public JsonNode applyPostHooks(JsonNode root) throws MigrationException {
		JsonNode ret = root;
		for (PostTransformHook hook : postTransformHooks) {
			ret = hook.applyHook(ret);
		}
		return ret;
	}

	/**
	 * add PostHooks in the correct order.
	 */
	protected abstract void initPostHooks();


	protected void addToObjectNode(JsonNode node, String key, String value) {
		((ObjectNode) node).put(key, value);
	}


	public ArrayList<PostTransformHook> getPostTransformHooks() {
		return postTransformHooks;
	}

	public void setPostTransformHooks(ArrayList<PostTransformHook> postTransformHooks) {
		this.postTransformHooks = postTransformHooks;
	}

	/**
	 * Create a Copy of Json and put nodes in user specified order
	 *
	 * @param target   new LinkedHashMap with wherer to put nodes
	 * @param source   source
	 * @param key      key to add to new HashMap
	 * @param children Specify Order on second level
	 */
	protected static void putObject(LinkedHashMap<Object, Object> target,
									LinkedHashMap<Object, Object> source,
									String key, String... children) throws MigrationException {

		Object obj = source.get(key);
		if (obj == null) {
			throw new MigrationException("Scenario must contain Key: " + key);
		}
		if (children.length > 0) {
			LinkedHashMap<Object, Object> node = new LinkedHashMap<>();
			LinkedHashMap<Object, Object> parent = (LinkedHashMap<Object, Object>) obj;
			for (String childKey : children) {
				Object childObj = parent.get(childKey);
				if (childObj == null) {
					throw new MigrationException("Object with Key " + key + " does not has child with key" + childKey);
				}
				node.put(childKey, childObj);
			}
			obj = node;
		}
		target.put(key, obj);

	}

	protected void remove(JsonNode root, String childName) throws MigrationException {
		ObjectNode parent = (ObjectNode) root;
		if (parent.remove(childName) == null) {
			throw new MigrationException("Cannot delete childElement '" + childName + "' from parent " + root.asText());
		}
	}

	protected JsonNode pathMustExist(JsonNode root, String path) throws MigrationException {
		JsonNode ret = path(root, path);
		if (ret.isMissingNode()) {
			throw new MigrationException("Json Path Erro: The path '" + path +
					"' should be accessible from " + root.asText());
		}
		return ret;
	}

	protected JsonNode path(JsonNode root, String path) {
		String[] pathElements = path.split("/");
		JsonNode ret = root;
		for (String item : pathElements) {
			ret = ret.path(item);
		}
		return ret;
	}

	protected boolean nodeIsArray(JsonNode node) {
		return nodeNotEmptyAnd(node, n -> n.getNodeType() == JsonNodeType.ARRAY);
	}

	protected boolean nodeNotEmptyAnd(JsonNode node, Predicate<JsonNode> predicate) {
		return !node.isMissingNode() && predicate.test(node);
	}

	protected Iterator<JsonNode> filterIterator(JsonNode root, Predicate<JsonNode> filter) {
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

}
