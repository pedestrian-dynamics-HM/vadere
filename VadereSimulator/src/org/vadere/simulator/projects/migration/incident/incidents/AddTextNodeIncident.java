package org.vadere.simulator.projects.migration.incident.incidents;


import org.vadere.simulator.projects.migration.incident.Tree;
import org.vadere.simulator.projects.migration.MigrationException;

import java.util.ArrayList;
import java.util.List;

public class AddTextNodeIncident extends Incident {

	private List<String> path;
	private String key;
	private String value;

	public AddTextNodeIncident(List<String> path, String key, String value) {
		this.path = path;
		this.key = key;
		this.value = value;
	}

	@Override
	public boolean applies(Tree graph) {
		List<String> pathIncludingKey = new ArrayList<>(path);
		pathIncludingKey.add(key);
		return !graph.pathExists(pathIncludingKey);
	}

	@Override
	public void resolve(Tree graph, StringBuilder log) throws MigrationException {
		super.stillApplies(graph);
		graph.createTextNode(path, key, value);
		log.append("\t- add text node [" + key + "] with value \"" + value + "\" to node " + graph.pathToString(path) + "\n");
	}
}
