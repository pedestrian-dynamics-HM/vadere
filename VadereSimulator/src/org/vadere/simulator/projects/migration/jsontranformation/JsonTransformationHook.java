package org.vadere.simulator.projects.migration.jsontranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.simulator.projects.migration.MigrationException;

@FunctionalInterface
public interface JsonTransformationHook {
	JsonNode applyHook (JsonNode root) throws MigrationException;
}
