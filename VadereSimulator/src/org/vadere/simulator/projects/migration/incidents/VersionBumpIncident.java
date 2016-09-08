package org.vadere.simulator.projects.migration.incidents;

import org.vadere.simulator.projects.migration.Graph;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.MigrationAssistant.Version;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class VersionBumpIncident extends Incident {

	private JsonNode node;
	private Version currentVersion;

	public VersionBumpIncident(JsonNode node, Version currentVersion) {
		this.node = node;
		this.currentVersion = currentVersion;
	}

	@Override
	public boolean applies(Graph graph) {
		return true;
	}

	@Override
	public void resolve(Graph graph, StringBuilder log) throws MigrationException {
		log.append("\t- change [release] version from \"" + currentVersion.label() + "\" to \""
				+ Version.latest().label() + "\"\n");
		((ObjectNode) node).put("release", Version.latest().label());
	}

}
