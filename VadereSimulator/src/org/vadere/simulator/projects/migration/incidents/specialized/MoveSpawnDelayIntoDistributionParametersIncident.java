package org.vadere.simulator.projects.migration.incidents.specialized;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.simulator.projects.migration.Graph;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.incidents.Incident;
import org.vadere.state.attributes.scenario.AttributesSource;

import static org.vadere.simulator.projects.migration.IncidentDatabase.path;


public class MoveSpawnDelayIntoDistributionParametersIncident extends Incident {

	@Override
	public boolean applies(Graph graph) {
		return true;
	}

	@Override
	public void resolve(Graph graph, StringBuilder log) throws MigrationException {

		Graph.Node sources_node = graph.getNodeByPath(path("vadere", "topography", "sources"));// null-check topography as well? must exit always, no?

		if (sources_node != null) {
			for (JsonNode source : sources_node.getJsonNode()) {
				if (source.has("spawnDelay")) {

					double spawnDelay = source.get("spawnDelay").asDouble();

					if (source.get("interSpawnTimeDistribution").asText().equals(AttributesSource.CONSTANT_DISTRIBUTION) && spawnDelay != -1.0) {
						((ObjectNode) source).set("distributionParameters", JsonConverter.toJsonNode(new Double[] {spawnDelay}));
					}

					((ObjectNode) source).remove("spawnDelay");
				}
			}
		}
	}

}
