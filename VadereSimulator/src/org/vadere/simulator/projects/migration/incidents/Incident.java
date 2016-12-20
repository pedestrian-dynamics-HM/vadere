package org.vadere.simulator.projects.migration.incidents;

import java.util.List;
import java.util.stream.Collectors;

import org.vadere.simulator.projects.migration.Tree;
import org.vadere.simulator.projects.migration.MigrationAssistant;
import org.vadere.simulator.projects.migration.MigrationException;

public abstract class Incident {

	public abstract boolean applies(Tree graph);

	public abstract void resolve(Tree graph, StringBuilder log) throws MigrationException;

	protected void stillApplies(Tree graph) throws MigrationException {
		if (!applies(graph))
			throw new MigrationException(this, MigrationAssistant.INCIDENT_ORDER_ERROR);
	}

	protected List<String> clone(List<String> list) {
		return list.stream().collect(Collectors.toList());
	}

}
