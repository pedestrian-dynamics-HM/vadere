package org.vadere.simulator.projects.migration.incident.incidents;

import java.util.List;
import java.util.stream.Collectors;

import org.vadere.simulator.projects.migration.incident.Tree;
import org.vadere.simulator.projects.migration.MigrationAssistant;
import org.vadere.simulator.projects.migration.MigrationException;

public abstract class Incident {

	public static final String INCIDENT_ORDER_ERROR = "An incident that was found applicable couldn't be resolved. " +
			"That means, that a previously resolved incident rendered this one no longer applicable. " +
			"Check the order of the incidents in the IncidentDatabase for logical flaws.";

	public abstract boolean applies(Tree graph);

	public abstract void resolve(Tree graph, StringBuilder log) throws MigrationException;

	protected void stillApplies(Tree graph) throws MigrationException {
		if (!applies(graph))
			throw new MigrationException(this, INCIDENT_ORDER_ERROR);
	}

	protected List<String> clone(List<String> list) {
		return list.stream().collect(Collectors.toList());
	}

}
