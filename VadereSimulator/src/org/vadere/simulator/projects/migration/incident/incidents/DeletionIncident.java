package org.vadere.simulator.projects.migration.incident.incidents;

import java.util.List;

import org.vadere.simulator.projects.migration.incident.Tree;
import org.vadere.simulator.projects.migration.MigrationException;

public class DeletionIncident extends Incident {

	private final List<String> path;

	public DeletionIncident(List<String> path) {
		this.path = path;
	}

	@Override
	public boolean applies(Tree graph) {
		return graph.pathExists(path);
	}

	@Override
	public void resolve(Tree graph, StringBuilder log) throws MigrationException {
		super.stillApplies(graph);
		log.append("\t- deleteEdge node " + graph.pathToString(path) + "\n");
		graph.deleteNode(path);
	}

}
