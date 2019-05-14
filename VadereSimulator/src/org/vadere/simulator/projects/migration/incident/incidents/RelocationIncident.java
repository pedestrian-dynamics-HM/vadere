package org.vadere.simulator.projects.migration.incident.incidents;

import java.util.List;

import org.vadere.simulator.projects.migration.incident.Tree;
import org.vadere.simulator.projects.migration.MigrationException;

public class RelocationIncident extends Incident {

	private final String key;
	private List<String> oldPath; // without key, just for the log-message
	private List<String> fullOldPath; // including key
	private final List<String> newPath; // without key

	/*
	 * to address ROOT directly, pass an empty array, not an array containing a ""-string
	 */
	public RelocationIncident(String key, List<String> oldPath, List<String> newPath) {
		this.key = key;
		this.oldPath = oldPath;
		this.fullOldPath = clone(oldPath);
		this.fullOldPath.add(key);
		this.newPath = newPath;
	}

	@Override
	public boolean applies(Tree graph) {
		return graph.pathExists(fullOldPath);
	}

	@Override
	public void resolve(Tree graph, StringBuilder log) throws MigrationException {
		super.stillApplies(graph);
		graph.relocateNode(fullOldPath, newPath);
		log.append("\t- attach node [" + key + "] to " + graph.pathToString(oldPath) + ", instead of "
				+ graph.pathToString(newPath) + "\n");
	}

}
