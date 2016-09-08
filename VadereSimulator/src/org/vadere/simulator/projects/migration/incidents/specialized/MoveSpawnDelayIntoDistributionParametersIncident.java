package org.vadere.simulator.projects.migration.incidents.specialized;

import static org.vadere.simulator.projects.migration.IncidentDatabase.path;

import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.simulator.projects.migration.Graph;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.incidents.Incident;
import org.vadere.state.attributes.scenario.AttributesSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class MoveSpawnDelayIntoDistributionParametersIncident extends Incident {

	@Override
	public boolean applies(Graph graph) {
		return true;
	}

	@Override
	public void resolve(Graph graph, StringBuilder log) throws MigrationException {

		Graph.Node sourcesNode = graph.getNodeByPath(path("vadere", "topography", "sources"));

		if (sourcesNode != null) {
			for (JsonNode source : sourcesNode.getJsonNode()) {
				if (source.has("spawnDelay")) {

					final double spawnDelay = source.get("spawnDelay").asDouble();
					final ObjectNode s = (ObjectNode) source;

					// If spawn delay is set AND constant spawn rate algorithm is used:
					// copy spawn delay to distribution parameters
					if (spawnDelay != -1.0 &&
							source.get("interSpawnTimeDistribution").asText()
							.equals(AttributesSource.CONSTANT_DISTRIBUTION)) {

						s.set("distributionParameters", JsonConverter.toJsonNode(new Double[] {spawnDelay}));
					}

					s.remove("spawnDelay");
				}
			}
		}
	}

}
