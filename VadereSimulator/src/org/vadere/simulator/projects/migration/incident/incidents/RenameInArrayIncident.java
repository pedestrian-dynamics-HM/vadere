package org.vadere.simulator.projects.migration.incident.incidents;

import java.util.List;

import org.vadere.simulator.projects.migration.incident.Tree;
import org.vadere.simulator.projects.migration.MigrationException;

public class RenameInArrayIncident extends Incident { // better name?

	private final List<String> pathToArray;
	private final String oldName;
	private final String newName;

	public RenameInArrayIncident(List<String> pathToArray, String oldName, String newName) {
		this.pathToArray = pathToArray;
		this.oldName = oldName;
		this.newName = newName;
	}

	@Override
	public boolean applies(Tree graph) {
		return graph.keyExistsInArray(pathToArray, oldName);
	}

	@Override
	public void resolve(Tree graph, StringBuilder log) throws MigrationException {
		super.stillApplies(graph);
		graph.renameKeyOccurrencesInArray(pathToArray, oldName, newName);
		log.append("\t- rename node [" + oldName + "] in array " + graph.pathToString(pathToArray) + " to [" + newName
				+ "]\n");
	}
}
