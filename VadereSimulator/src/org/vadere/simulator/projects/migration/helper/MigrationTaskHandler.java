package org.vadere.simulator.projects.migration.helper;

import java.nio.file.Path;

@FunctionalInterface
public interface MigrationTaskHandler {
	boolean handle(Path p);
}
