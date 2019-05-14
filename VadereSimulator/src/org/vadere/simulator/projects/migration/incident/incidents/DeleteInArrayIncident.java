package org.vadere.simulator.projects.migration.incident.incidents;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.incident.Tree;

import java.util.List;

public class DeleteInArrayIncident extends Incident{
	private final List<String> pathToArray;
	private final String key;

	public DeleteInArrayIncident(@NotNull final List<String> pathToArray, @NotNull final String key) {
		this.pathToArray = pathToArray;
		this.key = key;
	}

	@Override
	public boolean applies(@NotNull final Tree tree) {
		return tree.keyExistsInArray(pathToArray, key);
	}

	@Override
	public void resolve(@NotNull final Tree tree, @NotNull StringBuilder log) throws MigrationException {
		super.stillApplies(tree);
		tree.deleteNodeInArray(pathToArray, key);
		log.append("\t- deleteEdge node [" + key + "] in array " + Tree.pathToString(pathToArray) + "\n");
	}
}