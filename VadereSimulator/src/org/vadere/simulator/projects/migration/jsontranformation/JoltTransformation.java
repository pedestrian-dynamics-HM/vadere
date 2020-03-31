package org.vadere.simulator.projects.migration.jsontranformation;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.Diffy;
import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.logging.Logger;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public abstract class JoltTransformation extends AbstractJsonTransformation implements JsonTransformation, JsonNodeExplorer {

	protected final static Logger logger = Logger.getLogger(JoltTransformation.class);
	private static ConcurrentHashMap<Version, JoltTransformation> transformations = new ConcurrentHashMap<>();

	private final Chainr chainr; // Transformation from (version -1) to version
	private final Chainr identity; // Identity of version
	private final Diffy diffy;		// diff tool to check if jolt transformation worked.
	private final Version targetVersion;

	public static Path getTransformationFileFromFileSystem(Path baseDir, Version toVersion) {
		String transformString = getTransforamtionResourcePath(
				toVersion.previousVersion().label('-'),
				toVersion.label('-'));

		return baseDir.resolve(transformString.substring(1));
	}

	public static Path getIdenityFileFromFileSystem(Path baseDir, Version v) {
		String idenityString = getIdentiyResoucrePath(v.label('-'));
		return baseDir.resolve(idenityString.substring(1));
	}


	public static String getTransforamtionResourcePath(String from, String to) {
		return "/transform_v" + from.toUpperCase() + "_to_v" + to.toUpperCase() + ".json";
	}

	public static String getIdentiyResoucrePath(String to) {
		return "/identity_v" + to.toUpperCase() + ".json";
	}

	public JoltTransformation(String transformation, String identity, Version targetVersion) {
		super();
		this.chainr = Chainr.fromSpec(JsonUtils.classpathToList(transformation));
		this.identity = Chainr.fromSpec(JsonUtils.classpathToList(identity));
		this.diffy = new Diffy();
		this.targetVersion = targetVersion;
	}

	public JoltTransformation(Version targetVersion) {
		this(getTransforamtionResourcePath(
				targetVersion.previousVersion().label('-'),
				targetVersion.label('-')),
				getIdentiyResoucrePath(targetVersion.label('-')),
				targetVersion);
	}


	@Override
	public Version getTargetVersion() {
		return this.targetVersion;
	}

	@Override
	public JsonNode applyTransformation(JsonNode root) throws MigrationException {
		Object rootObject = StateJsonConverter.convertJsonNodeToObject(root);
		rootObject = applyTransformation(rootObject);
		JsonNode jsonRoot = StateJsonConverter.deserializeToNode(rootObject);
		return jsonRoot;
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

	/**
	 * add PostHooks in the correct order.
	 */
	protected abstract void initDefaultHooks();


}
