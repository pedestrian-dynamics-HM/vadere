package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;

import java.util.Random;


@MigrationTransformation(targetVersionLabel = "0.4")
public class JoltTransformV3toV4 extends JoltTransformation {
	public JoltTransformV3toV4() {
		super(Version.V0_4);
	}

	@Override
	protected void initPostHooks()  {
		postTransformHooks.add(this::presetSeedValues);
		postTransformHooks.add(JoltTransformV1toV2::sort);
	}

	public JsonNode presetSeedValues(JsonNode node) throws MigrationException {
			JsonNode attSim = node.findPath("scenario").findPath("attributesSimulation");
			if (attSim.isMissingNode())
				throw new MigrationException("attributesSimulation is not part of Scenario.");

			JsonNode fixedSeed = attSim.findPath("fixedSeed");
			if (fixedSeed.isMissingNode())
				throw new MigrationException("scenario.attributesSimulation.fixedSeed must be present" +
						"for Version V0.4");

			Integer fixedSeedVal = fixedSeed.asInt(-1);
			if (fixedSeedVal == -1){
				((ObjectNode)attSim).put("fixedSeed", new Random().nextLong());
			}

			return node;
	}
}
