package org.vadere.simulator.projects.migration.incidents.specialized;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.vadere.simulator.models.potential.PotentialFieldObstacleCompact;
import org.vadere.simulator.models.potential.PotentialFieldObstacleOSM;
import org.vadere.simulator.models.potential.PotentialFieldPedestrianCompact;
import org.vadere.simulator.models.potential.PotentialFieldPedestrianOSM;
import org.vadere.simulator.projects.migration.Graph;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.incidents.Incident;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.models.AttributesPotentialCompact;
import org.vadere.state.attributes.models.AttributesPotentialOSM;

import static org.vadere.simulator.projects.migration.IncidentDatabase.path;

/**
 * For one particular case, not generalizable.
 */
public class AttributesPotentialCompactVSosmIncident extends Incident {

	@Override
	public boolean applies(Graph graph) {
		return true;
	}

	@Override
	public void resolve(Graph graph, StringBuilder log) throws MigrationException {

		Graph.Node AttributesOSM_node = graph.getNodeByPath(path("vadere", "attributesModel", AttributesOSM.class.getName()));

		if (AttributesOSM_node != null) {

			Graph.Node AttributesPotentialCompact_node = graph.getNodeByPath(path("vadere", "attributesModel", AttributesPotentialCompact.class.getName()));
			Graph.Node AttributesPotentialOSM_node = graph.getNodeByPath(path("vadere", "attributesModel", AttributesPotentialOSM.class.getName()));

			if (AttributesPotentialCompact_node != null && AttributesPotentialOSM_node != null) {
				throw new MigrationException(this, "Both AttributesPotentialCompact and AttributesPotentialOSM are present, that is not allowed.");
			}

			if (AttributesPotentialCompact_node != null) {
				((ObjectNode) AttributesOSM_node.getJsonNode()).put("pedestrianPotentialModel", PotentialFieldPedestrianCompact.class.getName());
				((ObjectNode) AttributesOSM_node.getJsonNode()).put("obstaclePotentialModel", PotentialFieldObstacleCompact.class.getName());
				log.append("\t- AttributesOSM: since AttributesPotentialCompact is present, set [pedestrianPotentialModel] to PotentialFieldPedestrianCompact " +
						"and [obstaclePotentialModel] to PotentialFieldObstacleCompact" + "\n");
			}

			if (AttributesPotentialOSM_node != null) {
				((ObjectNode) AttributesOSM_node.getJsonNode()).put("pedestrianPotentialModel", PotentialFieldPedestrianOSM.class.getName());
				((ObjectNode) AttributesOSM_node.getJsonNode()).put("obstaclePotentialModel", PotentialFieldObstacleOSM.class.getName());
				log.append("\t- AttributesOSM: since AttributesPotentialOSM is present, set [pedestrianPotentialModel] to PotentialFieldPedestrianOSM " +
						"and [obstaclePotentialModel] to PotentialFieldObstacleOSM" + "\n");
			}
		}
	}
}
