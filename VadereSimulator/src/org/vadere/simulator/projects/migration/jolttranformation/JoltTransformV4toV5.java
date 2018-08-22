package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;

import java.util.Iterator;

@MigrationTransformation(targetVersionLabel = "0.5")
public class JoltTransformV4toV5 extends JoltTransformation {

	public JoltTransformV4toV5() {
		super(Version.V0_5);
	}

	@Override
	protected void initPostHooks() {
		postTransformHooks.add(this::cleanupPedestrianOverlapProcessorAttribute);
		postTransformHooks.add(this::addOverlapProcessors);
		postTransformHooks.add(JoltTransformV1toV2::sort);
	}

	private JsonNode cleanupPedestrianOverlapProcessorAttribute(JsonNode node) throws MigrationException {
		JsonNode processors = path(node, "processWriters/processors");
		if (nodeIsArray(processors)) {
			Iterator<JsonNode> iter = processors.iterator();
			while (iter.hasNext()) {
				JsonNode processor = iter.next();
				String type = pathMustExist(processor, "type").asText();
				if (type.equals("org.vadere.simulator.projects.dataprocessing.processor.PedestrianOverlapProcessor")) {
					remove(processor, "attributes");
					remove(processor, "attributesType");
				}
			}
		}
		return node;
	}

	private JsonNode addOverlapProcessors(JsonNode node) throws MigrationException {
		return node;
	}


}
