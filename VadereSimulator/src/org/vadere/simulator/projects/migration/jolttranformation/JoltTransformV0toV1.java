package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.models.gnm.GradientNavigationModel;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.simulator.models.potential.PotentialFieldObstacleCompact;
import org.vadere.simulator.models.potential.PotentialFieldObstacleOSM;
import org.vadere.simulator.models.potential.PotentialFieldPedestrianCompact;
import org.vadere.simulator.models.potential.PotentialFieldPedestrianOSM;
import org.vadere.simulator.models.sfm.SocialForceModel;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.state.attributes.models.AttributesGNM;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.models.AttributesPotentialCompact;
import org.vadere.state.attributes.models.AttributesPotentialOSM;
import org.vadere.state.attributes.scenario.AttributesSource;

import java.util.Iterator;

public class JoltTransformV0toV1 extends JoltTransformation {


	JoltTransformV0toV1(String transformation, String identity, Version version) {
		super(transformation, identity, version);
	}

	@Override
	protected void initPostHooks() {
		postTransformHooks.add(this::findMainModel);
		postTransformHooks.add(this::attributesPotentialCompactVSosmIncident);
		postTransformHooks.add(this::moveSpawnDelayIntoDistributionParametersIncident);
	}

	private JsonNode findMainModel(JsonNode node) throws MigrationException {
		JsonNode scenario = node.path("scenario");
		if (scenario.isMissingNode()){
			logger.error("There must be scenario Node");
			throw new MigrationException("There must be scenario Node");
		}

		// Possible Main Models
		String mainModelValue = "";
		if (! node.path("scenario").path("attributesModel").path(AttributesOSM.class.getName()).isMissingNode()){
			mainModelValue = OptimalStepsModel.class.getName();
		}

		if (! node.path("scenario").path("attributesModel").path(AttributesGNM.class.getName()).isMissingNode()){
			if (!mainModelValue.equals("")){
				throw new MigrationException("can't automatically determine the mainModel - more than one mainModel-suitable model is present");
			}
			mainModelValue = GradientNavigationModel.class.getName();
		}

		if (! node.path("scenario").path("attributesModel").path(AttributesGNM.class.getName()).isMissingNode()){
			if (!mainModelValue.equals("")){
				throw new MigrationException("can't automatically determine the mainModel - more than one mainModel-suitable model is present");
			}
			mainModelValue = SocialForceModel.class.getName();
		}

		if (mainModelValue.equals("")){
			throw new MigrationException("couldn't automatically determine the mainModel based on the present models OSM, GNM, SFM");
		}

		addToObjectNode(scenario, "mainModel", mainModelValue);

		return node;
	}

	/**
	 *  Set specific fields within the AttributesOSM depending on other present Attributes. This
	 *  Transformation is context sensitive thus handle this with a hook and not within the
	 *  Jolt transformation
	 */
	private JsonNode attributesPotentialCompactVSosmIncident (JsonNode node) throws MigrationException {
		JsonNode osmAttr = node.path("scenario").path("attributesModel").path(AttributesOSM.class.getCanonicalName());
		if (osmAttr.isMissingNode())
			return node;

		JsonNode potentialCompactAttr = node.path("scenario").path("attributesModel").path(AttributesPotentialCompact.class.getCanonicalName());
		JsonNode potentialOSMAttr = node.path("scenario").path("attributesModel").path(AttributesPotentialOSM.class.getCanonicalName());

		if (!potentialCompactAttr.isMissingNode() && !potentialOSMAttr.isMissingNode()){
			throw new MigrationException("[AttributesPotentialCompact] and [AttributesPotentialOSM] are both present, that is not allowed.");
		}

		String beforeChange = osmAttr.toString();

		if (!potentialCompactAttr.isMissingNode()){
			addToObjectNode(osmAttr, "pedestrianPotentialModel", PotentialFieldPedestrianCompact.class.getName());
			addToObjectNode(osmAttr, "obstaclePotentialModel", PotentialFieldObstacleCompact.class.getName());
			if (!beforeChange.equals(osmAttr.toString())) {
				logger.info("\t- AttributesOSM: since AttributesPotentialCompact is present, set [pedestrianPotentialModel] to PotentialFieldPedestrianCompact " +
						"and [obstaclePotentialModel] to PotentialFieldObstacleCompact" + "\n");
			}
		}

		if (!potentialOSMAttr.isMissingNode()){
			addToObjectNode(osmAttr, "pedestrianPotentialModel", PotentialFieldPedestrianOSM.class.getName());
			addToObjectNode(osmAttr, "obstaclePotentialModel", PotentialFieldObstacleOSM.class.getName());
			if (!beforeChange.equals(osmAttr.toString())) {
				logger.info("\t- AttributesOSM: since AttributesPotentialOSM is present, set [pedestrianPotentialModel] to PotentialFieldPedestrianOSM " +
						"and [obstaclePotentialModel] to PotentialFieldObstacleOSM" + "\n");
			}
		}

		return node;
	}


	/**
	 *  If the interSpawnTimeDistribution is set within one source rebuild the distributionParameters
	 */
	private JsonNode moveSpawnDelayIntoDistributionParametersIncident (JsonNode node) {
		JsonNode sources = node.path("scenario").path("topography").path("sources");

		if (sources.isMissingNode())
			return node;

		Iterator<JsonNode> iter = sources.elements();
		//apply for reach source
		while (iter.hasNext()){
			JsonNode source = iter.next();
			final double spawnDelay = source.path("spawnDelay").asDouble();
			JsonNode distribution = source.path("interSpawnTimeDistribution");

			if (spawnDelay != -1.0 && (!distribution.isMissingNode() ||
					distribution.asText().equals(AttributesSource.CONSTANT_DISTRIBUTION))){
				setDoubelArray(source, "distributionParameters",  spawnDelay);
			}

			((ObjectNode)source).remove("spawnDelay");
		}

		return node;
	}
}
