package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.simulator.projects.migration.MigrationException;

@FunctionalInterface
public interface PostTransformHook {
	JsonNode applyHook (JsonNode root) throws MigrationException;
}
