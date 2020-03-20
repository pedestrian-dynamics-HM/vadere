package org.vadere.simulator.projects.migration.jsontranformation.jolt;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.JoltTransformation;

import java.util.Iterator;

@MigrationTransformation(targetVersionLabel = "0.5")
public class JoltTransformV4toV5 extends JoltTransformation {

	public JoltTransformV4toV5() {
		super(Version.V0_5);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookLast(this::cleanupPedestrianOverlapProcessorAttribute);
		addPostHookLast(this::sort);
	}

	private JsonNode cleanupPedestrianOverlapProcessorAttribute(JsonNode node) throws MigrationException {
		Iterator<JsonNode> iter = iteratorProcessorsByType(node,"org.vadere.simulator.projects.dataprocessing.processor.PedestrianOverlapProcessor");
		while (iter.hasNext()) {
			JsonNode processor = iter.next();
			String type = pathMustExist(processor, "type").asText();
				remove(processor, "attributes");
				remove(processor, "attributesType");
		}
		return node;
	}



}
