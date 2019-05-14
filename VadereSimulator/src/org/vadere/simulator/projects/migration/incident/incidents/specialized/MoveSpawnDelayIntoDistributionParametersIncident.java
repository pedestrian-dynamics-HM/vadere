package org.vadere.simulator.projects.migration.incident.incidents.specialized;

import static org.vadere.simulator.projects.migration.incident.IncidentDatabase.path;

import org.vadere.simulator.projects.migration.incident.Tree;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.incident.incidents.Incident;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.util.StateJsonConverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class MoveSpawnDelayIntoDistributionParametersIncident extends Incident {

	@Override
	public boolean applies(Tree graph) {
		return true;
	}

	@Override
	public void resolve(Tree graph, StringBuilder log) throws MigrationException {

		Tree.Node sourcesNode = graph.getNodeByPath(path("vadere", "topography", "sources"));

		if (sourcesNode != null) {
			for (JsonNode source : sourcesNode.getJsonNode()) {
				if (source.has("spawnDelay")) {

					final double spawnDelay = source.get("spawnDelay").asDouble();
					final ObjectNode s = (ObjectNode) source;
					final JsonNode distribution = source.get("interSpawnTimeDistribution");

					// If spawn delay is set AND constant spawn rate algorithm is used:
					// copy spawn delay to distribution parameters
					if (spawnDelay != -1.0 &&
							(distribution == null
							|| distribution.asText().equals(AttributesSource.CONSTANT_DISTRIBUTION))) {

						s.set("distributionParameters", StateJsonConverter.toJsonNode(new Double[] {spawnDelay}));
					}

					s.remove("spawnDelay");
				}
			}
		}
	}

}
