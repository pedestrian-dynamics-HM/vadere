package org.vadere.simulator.projects.migration.jolttranformation;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.Diffy;
import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.log4j.Logger;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.state.util.StateJsonConverter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;


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


		String transformationResource = "/transform_v" + currentVersion.label('-').toUpperCase() + "_to_v" + currentVersion.nextVersion().label('-').toUpperCase() + ".json";
		String identityResource = "/identity_v" + currentVersion.nextVersion().label('-').toUpperCase() + ".json";

		JoltTransformation ret = transformations.getOrDefault(currentVersion, null);
		if ( ret == null) {
			switch (currentVersion) {
				case NOT_A_RELEASE:
					ret = new JoltTransformV0toV1(transformationResource, identityResource, currentVersion);
					break;
				case V0_1:
					ret = new JoltTransformV1toV2(transformationResource, identityResource, currentVersion);
					break;
				case V0_2:
					ret = new JoltTransformV2toV3(transformationResource, identityResource, currentVersion);
					break;
				default:
					throw new MigrationException("No Transformation defined for Verson " + currentVersion.toString());
			}
			transformations.put(currentVersion, ret);
		}
		return ret;
	}

	public JoltTransformation(String transformation, String  identity, Version version) throws MigrationException {
		this.chainr = Chainr.fromSpec(JsonUtils.classpathToList(transformation));
		this.identity = Chainr.fromSpec(JsonUtils.classpathToList(identity));
		// Output of Transformation
		this.postTransformHooks = new ArrayList<>();
		this.diffy = new Diffy();
		initPostHooks();
	}

	public JsonNode applyTransformation(JsonNode root) throws MigrationException {
		Object rootObject = StateJsonConverter.convertJsonNodeToObject(root);
		rootObject = applyTransformation(rootObject);
		JsonNode jsonRoot = StateJsonConverter.deserializeToNode(rootObject);
		return  applyPostHooks(jsonRoot);
	}

	private Object applyTransformation(Object root) throws MigrationException {
		Object rootTransformed = chainr.transform(root);
		Object rootTransformedIdenity = identity.transform(rootTransformed);

		Diffy.Result diffResult = diffy.diff(rootTransformed, rootTransformedIdenity);
		if (diffResult.isEmpty()){
			return rootTransformed;
		} else {
			logger.error("Error in Transformation " + diffResult.toString());
			throw new MigrationException("Error in Transformation " + diffResult.toString());
		}
	}

	public JsonNode applyPostHooks(JsonNode root) throws MigrationException{
		JsonNode ret = root;
		for (PostTransformHook hook : postTransformHooks) {
			ret = hook.applyHook(ret);
		}
		return ret;
	}

	/**
	 * add PostHooks in the correct order.
	 */
	protected abstract void initPostHooks() throws MigrationException;


	protected void addToObjectNode(JsonNode node, String key, String value){
		((ObjectNode)node).put(key, value);
	}


	public ArrayList<PostTransformHook> getPostTransformHooks() {
		return postTransformHooks;
	}

	public void setPostTransformHooks(ArrayList<PostTransformHook> postTransformHooks) {
		this.postTransformHooks = postTransformHooks;
	}

	/**
	 * Create a Copy of Json and put nodes in user specified order
	 * @param target		new LinkedHashMap with wherer to put nodes
	 * @param source		source
	 * @param key			key to add to new HashMap
	 * @param children		Specify Order on second level
	 */
	protected void putObject(LinkedHashMap<Object, Object> target,
						   LinkedHashMap<Object, Object> source,
						   String key, String... children) throws MigrationException {

		Object obj = source.get(key);
		if (obj == null) {
			throw new MigrationException("Scenario must contain Key: " + key);
		}
		if (children.length > 0){
			LinkedHashMap<Object, Object> node = new LinkedHashMap<>();
			LinkedHashMap<Object, Object> parent = (LinkedHashMap<Object, Object>) obj;
			for (String childKey : children){
				Object childObj = parent.get(childKey);
				if (childObj == null){
					throw new MigrationException("Object with Key " + key + " does not has child with key" + childKey);
				}
				node.put(childKey, childObj);
			}
			obj = node;
		}
		target.put(key, obj);

	}
}
