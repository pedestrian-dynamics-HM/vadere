package org.vadere.simulator.projects.migration.jsontranformation.jolt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.JoltTransformation;
import org.vadere.state.attributes.AttributesSimulation;

import java.util.Random;


@MigrationTransformation(targetVersionLabel = "0.4")
public class JoltTransformV3toV4 extends JoltTransformation {
	public JoltTransformV3toV4() {
		super(Version.V0_4);
	}

	@Override
	protected void initDefaultHooks()  {
        addPostHookLast(this::presetSeedValues);
		addPostHookLast(this::sort);
	}

	public JsonNode presetSeedValues(JsonNode node) throws MigrationException {
			JsonNode attSim = node.findPath("scenario").findPath(AttributesSimulation.JSON_KEY);
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
