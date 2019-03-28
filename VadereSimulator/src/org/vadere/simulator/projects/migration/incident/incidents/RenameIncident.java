package org.vadere.simulator.projects.migration.incident.incidents;

import java.util.List;

import org.vadere.simulator.projects.migration.incident.Tree;
import org.vadere.simulator.projects.migration.MigrationException;

public class RenameIncident extends Incident {

	private final List<String> path;
	private final String newName;

	public RenameIncident(List<String> path, String newName) {
		this.path = path;
		this.newName = newName;
	}

	@Override
	public boolean applies(Tree graph) {
		return graph.pathExists(path);
	}

	@Override
	public void resolve(Tree graph, StringBuilder log) throws MigrationException {
		super.stillApplies(graph);
		graph.renameNode(path, newName);
		log.append("\t- rename node " + graph.pathToString(path) + " to [" + newName + "]\n");
	}
}
