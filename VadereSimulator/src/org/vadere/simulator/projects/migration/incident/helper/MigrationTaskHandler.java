package org.vadere.simulator.projects.migration.incident.helper;

import java.nio.file.Path;

@FunctionalInterface
public interface MigrationTaskHandler {
	boolean handle(Path p);
}
