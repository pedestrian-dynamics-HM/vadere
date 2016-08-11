package org.vadere.simulator.projects.migration.incidents;

import org.vadere.simulator.projects.migration.Graph;
import org.vadere.simulator.projects.migration.MigrationAssistant;
import org.vadere.simulator.projects.migration.MigrationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class VersionBumpIncident extends Incident {

	private JsonNode node;
	private String currentVersion;

	public VersionBumpIncident(JsonNode node, String currentVersion) {
		this.node = node;
		this.currentVersion = currentVersion;
	}

	@Override
	public boolean applies(Graph graph) {
		return true;
	}

	@Override
	public void resolve(Graph graph, StringBuilder log) throws MigrationException {
		log.append("\t- change [release] version from \"" + currentVersion + "\" to \""
				+ MigrationAssistant.latestVersion + "\"\n");
		((ObjectNode) node).put("release", MigrationAssistant.latestVersion);
	}

}
