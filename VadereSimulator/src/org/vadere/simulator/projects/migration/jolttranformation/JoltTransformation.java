package org.vadere.simulator.projects.migration.jolttranformation;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.Diffy;
import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.log4j.Logger;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.incidents.VersionBumpIncident;
import org.vadere.state.util.StateJsonConverter;

import java.util.ArrayList;


public abstract class JoltTransformation {

	protected final static Logger logger = Logger.getLogger(JoltTransformation.class);

	private final Chainr chainr; // Transformation from (version -1) to version
	private final Chainr identity; // Identity of version
	private final Version version; // Output of Transformation
	private final Diffy diffy;
	protected ArrayList<PostTransformHook> postTransformHooks;


	public static JoltTransformation get(Version v) throws MigrationException {
		if (v.equalOrSamller(Version.UNDEFINED))
			throw new MigrationException("There is now Transformation for Version " + Version.UNDEFINED.toString());

		if (v.equals(Version.latest()))
			throw new MigrationException("No Transformation needed. Already latest Version!");


		String transformationResource = "transform_v" + v.label('-').toUpperCase() + "_to_v" + v.nextVersion().label('-').toUpperCase() + ".json";
		String identityResource = "identity_v" + v.nextVersion().label('-').toUpperCase() + ".json";

		JoltTransformation ret = null;

		switch (v) {
			case NOT_A_RELEASE:
				ret = new JoltTransformV0toV1(transformationResource, identityResource, v);
				break;
			case V0_1:
				ret = new JoltTransformV1toV2(transformationResource, identityResource, v);
				break;
			case V0_2:
				ret = new JoltTransformV2toV3(transformationResource, identityResource, v);
				break;
		}

		if (ret == null)
			throw new MigrationException("No Transformation defined for Verson " + v.toString());

		return ret;
	}

	public JoltTransformation(String transformation, String  identity, Version version){
		this.chainr = Chainr.fromSpec(JsonUtils.classpathToList(transformation));
		this.identity = Chainr.fromSpec(JsonUtils.classpathToList(identity));
		this.version = version;
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

	private JsonNode applyPostHooks(JsonNode root) throws MigrationException{
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


	protected void addToObjectNode(JsonNode node, String key, String value){
		((ObjectNode)node).put(key, value);
	}
	protected void setDoubelArray(JsonNode node, String key, Double value){
		((ObjectNode)node).set(key, StateJsonConverter.toJsonNode(new Double[] {value}));
	}

	public ArrayList<PostTransformHook> getPostTransformHooks() {
		return postTransformHooks;
	}

	public void setPostTransformHooks(ArrayList<PostTransformHook> postTransformHooks) {
		this.postTransformHooks = postTransformHooks;
	}
}
