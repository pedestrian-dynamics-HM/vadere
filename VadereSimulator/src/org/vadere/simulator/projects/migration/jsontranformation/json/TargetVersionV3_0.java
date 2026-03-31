package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.util.version.Version;

import java.util.LinkedList;

/**
 * Simply upgrades version number to 3.0; All other changes have no effect on the scenario file; 3.0 release provides
 * new generation of Vadere developers with a stable version. This is the reason for increasing the major release number
 * instead of the minor release number. There are no major changes.
 */

@MigrationTransformation(targetVersionLabel =  "3.0")
public class TargetVersionV3_0 extends SimpleJsonTransformation {

	public TargetVersionV3_0() {
		super(Version.V3_0);
	}

	@Override
	protected void initDefaultHooks() {
		// addPostHookFirst(this::someMethodDoingStuffToTheJsonNodesInScenarioFiles);
		// addPreHookLast(this::sort); // here not required because there are no changes to the scenario file
	}
}
