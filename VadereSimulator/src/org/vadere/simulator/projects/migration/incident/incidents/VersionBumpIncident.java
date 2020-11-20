package org.vadere.simulator.projects.migration.incident.incidents;

import org.vadere.simulator.projects.migration.incident.Tree;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;

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
	public boolean applies(Tree graph) {
		return true;
	}

	@Override
	public void resolve(Tree graph, StringBuilder log) throws MigrationException {
		log.append("\t- change [release] version from \"" + currentVersion.label() + "\" to \""
				+ Version.latest().label() + "\"\n");
		((ObjectNode) node).put("release", Version.latest().label());
	}

}
